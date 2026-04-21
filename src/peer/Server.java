import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Responsible for accepting connection requests from clients,
 * and sending file data to other clients.
 */
public class Server extends Thread {
    private final int port;
    private final Peer peerRef;
    private final Timer timer = new Timer();
    private Set<Integer> unchokedNeighbors = new HashSet<>();
    private Optional<Integer> optimisticUnchokedNeighbor = Optional.empty();
    private Map<Integer, Handler> handlers = new ConcurrentHashMap<>();

    public Server(Peer peerRef) {
        this.peerRef = peerRef;
        this.port = this.peerRef.peerConfig.get(this.peerRef.id).port();
    }

    /**
     * Start server to accept incoming client connection requests
     */
    public void run() {
        try {
            try (ServerSocket listener = new ServerSocket(this.port)) {
                System.out.println("Started listening on: " + this.port);

                // Set up rates map
                refreshNeighborsRates();

                // Set up unchoking intervals
                timer.schedule(new TimerTask() {
                    public void run() {
                        Set<Integer> prev = new HashSet<>(unchokedNeighbors);
                        unchokedNeighbors = findNeighborsTopRates();
                        sendChokeMessages();
                        if (!prev.equals(unchokedNeighbors)) {
                            peerRef.fileLogger.logPreferredNeighbors(new ArrayList<>(unchokedNeighbors));
                        }
                        // System.out.println("Unchoked Neighbors: " + unchokedNeighbors);
                    }
                }, 0, this.peerRef.commonConfig.unchokingInterval() * 1000);

                timer.schedule(new TimerTask() {
                    public void run() {
                        Optional<Integer> prev = optimisticUnchokedNeighbor;
                        optimisticUnchokedNeighbor = findOptimisticUnchokedNeighbor();

                        if (prev.isEmpty()) {
                            if (optimisticUnchokedNeighbor.isPresent()
                                    && handlers.containsKey(optimisticUnchokedNeighbor.get())) {
                                handlers.get(optimisticUnchokedNeighbor.get()).sendChoke(false);
                                peerRef.fileLogger
                                        .logOptimisticallyUnchokedNeighbor(optimisticUnchokedNeighbor.get());
                            }
                        } else {
                            if (optimisticUnchokedNeighbor.isEmpty() && handlers.containsKey(prev.get())) {
                                handlers.get(prev.get()).sendChoke(true);
                            } else if (optimisticUnchokedNeighbor.isPresent()) {
                                Integer prevPeerId = prev.get();
                                Integer newPeerId = optimisticUnchokedNeighbor.get();
                                if (!prevPeerId.equals(newPeerId)) {
                                    if (!unchokedNeighbors.contains(prevPeerId) && handlers.containsKey(prevPeerId)) {
                                        handlers.get(prevPeerId).sendChoke(true);
                                    }
                                    if (handlers.containsKey(newPeerId)) {
                                        handlers.get(newPeerId).sendChoke(false);
                                        peerRef.fileLogger.logOptimisticallyUnchokedNeighbor(newPeerId);
                                    }
                                }
                            }
                        }

                        // System.out.println("Optimistic Unchoked Neighbor: " +
                        // optimisticUnchokedNeighbor);
                    }
                }, 0, this.peerRef.commonConfig.optimisticUnchokingInterval() * 1000);

                while (true) {
                    // Receive new connection when needed
                    Handler handler = new Handler(listener.accept(), this.peerRef);
                    handler.start();

                    // Tell peer about connection
                    this.peerRef.recordNewServerConnection(handler.peerId, true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            this.peerRef.fileMaker.closeConnection();
            this.peerRef.fileReader.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Spawns from the listening loop and
     * are responsible for dealing with a single client's requests.
     */
    private class Handler extends Thread {
        private Socket connection;
        private Integer peerId;
        private Peer peerRef;

        public Handler(Socket connection, Peer peerRef) throws IOException {
            this.connection = connection;
            this.peerRef = peerRef;

            InputStream input = connection.getInputStream();
            while (true) {
                try {
                    this.peerId = Message.decodeHandshake(input);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            if (!this.peerRef.client.getConnectedTo().containsKey(this.peerId)) {
                this.peerRef.fileLogger.logConnectionFrom(this.peerId);
            }
            handlers.put(peerId, this);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(Message.encodeHandshake(this.peerRef.id));
            outputStream.write(Message.encodeBitfield(this.peerRef.bitfield));
        }

        /**
         * Handles connection from other peer's client,
         * called by Thread.start()
         */
        public void run() {
            while (true) {
                try {
                    InputStream input = this.connection.getInputStream();

                    Message message = Message.decodeMessage(input);

                    Boolean interest;
                    Integer pieceIndex;
                    switch (message.type()) {
                        case Message.Type.INTERESTED:
                            interest = Message.decodeInterest(message);
                            assert interest;

                            this.peerRef.neighborsInterested.add(this.peerId);

                            this.peerRef.fileLogger.logReceivedInterested(this.peerId);
                            break;

                        case Message.Type.NOT_INTERESTED:
                            interest = Message.decodeInterest(message);
                            assert !interest;

                            this.peerRef.neighborsInterested.remove(this.peerId);

                            this.peerRef.fileLogger.logReceivedNotInterested(this.peerId);
                            break;

                        case Message.Type.REQUEST:
                            pieceIndex = Message.decodeIndexField(message);
                            if (unchokedNeighbors.contains(this.peerId) || (optimisticUnchokedNeighbor.isPresent()
                                    && optimisticUnchokedNeighbor.get().equals(this.peerId))) {
                                byte[] fileData = this.peerRef.fileReader.getPiece(pieceIndex);
                                connection.getOutputStream().write(Message.encodePiece(
                                        new Message.PieceData(pieceIndex, fileData),
                                        this.peerRef.commonConfig.pieceSize()));
                            }
                            break;

                        case Message.Type.HAVE:
                            pieceIndex = Message.decodeIndexField(message);
                            List<Boolean> otherBitfield = this.peerRef.neighborBitfields.get(peerId);

                            if (!this.peerRef.hasFile) {
                                otherBitfield.set(pieceIndex, true);
                            }

                            this.peerRef.recordHave();
                            this.peerRef.fileLogger.logReceivedHave(this.peerId, pieceIndex);
                            break;

                        default:
                            System.out.println(message.type());
                            break;
                    }

                } catch (SocketTimeoutException timeout) {
                    continue;
                } catch (SocketException e) {
                    e.printStackTrace();
                    try {
                        handlers.remove(this.peerId);
                        this.connection.close();
                        return;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return;
                } catch (BufferUnderflowException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendChoke(Boolean choked) {
            try {
                connection.getOutputStream().write(Message.encodeChoke(choked));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(peerId);
        }

    }

    /**
     * Refreshes rates for each choking interval
     */
    private void refreshNeighborsRates() {
        this.peerRef.ratesFromNeighbors.clear();
        for (Integer peerId : this.handlers.keySet()) {
            this.peerRef.ratesFromNeighbors.put(peerId, BigInteger.ZERO);
        }
    }

    /**
     * Finds the neighbors with the highest send rates to this peer
     * 
     * @return
     */
    private Set<Integer> findNeighborsTopRates() {
        // Only considers neighbors that are interested in pieces this peer has
        List<Entry<Integer, BigInteger>> entries = new ArrayList<>(this.peerRef.ratesFromNeighbors.entrySet()).stream()
                .filter(entry -> this.peerRef.neighborsInterested.contains(entry.getKey()))
                .collect(Collectors.toList());

        // Clear out previous values
        refreshNeighborsRates();

        // Randomization if two neighbors have the same rate
        Collections.shuffle(entries);

        if (!this.peerRef.hasFile) {
            Collections.sort(entries, Entry.comparingByValue());
        }

        int topNeighborsStartingIndex = entries.size() - this.peerRef.commonConfig.numberOfPreferredNeighbors();
        if (topNeighborsStartingIndex < 0) {
            topNeighborsStartingIndex = 0;
        }

        // Finds N top preferred neighbors
        Set<Integer> topNeighbors = entries.isEmpty() ? Set.of()
                : new HashSet<>(entries
                        .subList(topNeighborsStartingIndex, entries.size())
                        .stream()
                        .map(entry -> entry.getKey())
                        .toList());

        // Moves optimistic unchoked to normal unchoked
        if (optimisticUnchokedNeighbor.isPresent()) {
            if (topNeighbors.contains(optimisticUnchokedNeighbor.get())) {
                optimisticUnchokedNeighbor = Optional.empty();
            }
        }

        // System.out.println(topNeighbors);
        return topNeighbors;
    }

    /**
     * Finds a neighbor each optimistic unchoking interval that is currently choked
     * 
     * @return
     */
    private Optional<Integer> findOptimisticUnchokedNeighbor() {
        Set<Integer> unchoked = new HashSet<>();
        for (Integer peerId : unchokedNeighbors) {
            unchoked.add(peerId);
        }
        if (optimisticUnchokedNeighbor.isPresent()) {
            unchoked.add(optimisticUnchokedNeighbor.get());
        }

        Set<Integer> interestedNeighbors = new HashSet<>(this.peerRef.neighborsInterested);
        if (interestedNeighbors.isEmpty()) {
            return Optional.empty();
        }

        if (!interestedNeighbors.removeAll(unchoked)) {
            return Optional.empty();
        }

        List<Integer> choked = new ArrayList<>(interestedNeighbors);
        return choked.isEmpty() ? Optional.empty() : Optional.of(choked.get(new Random().nextInt(choked.size())));
    }

    /**
     * Sends choking messages to normally unchoked or choked peers. Optimistic
     * neighbors are dealt with separately in their interval.
     * 
     * @throws IOException
     */
    private void sendChokeMessages() {
        for (Entry<Integer, Handler> entry : this.handlers.entrySet()) {
            if (optimisticUnchokedNeighbor.isPresent() && entry.getKey().equals(optimisticUnchokedNeighbor.get())) {
                continue;
            }
            Boolean choked = !this.unchokedNeighbors.contains(entry.getKey());
            entry.getValue().sendChoke(choked);
        }
    }

}
