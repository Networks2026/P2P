import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Client extends Thread {
    private final Peer peerRef;

    private Map<Integer, Socket> requestSockets = new ConcurrentHashMap<>();

    // This may not be needed, not sure yet
    private Set<Integer> unchokedBy = ConcurrentHashMap.newKeySet();

    private Map<Integer, Handler> handlers = new ConcurrentHashMap<>();

    public Client(Peer peerRef) {
        this.peerRef = peerRef;
    };

    public void connectTo(int peerId, Boolean noLog) throws UnknownHostException, IOException {
        if (requestSockets.containsKey(peerId)) {
            return;
        }

        System.out.println("Attempting to connect to peer: " + peerId);

        PeerConfigData peerData = this.peerRef.peerConfig.get(peerId);
        Socket newSocket = new Socket(peerData.hostName(), peerData.port());

        OutputStream outputStream = newSocket.getOutputStream();
        outputStream.write(Message.encodeHandshake(this.peerRef.id));

        InputStream inputStream = newSocket.getInputStream();
        while (true) {
            try {
                Integer incomingPeerId = Message.decodeHandshake(inputStream);
                if (incomingPeerId != peerId) {
                    System.out.println("Incorrect peer attempting to connect");
                    break;
                } else {
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        if (!noLog) {
            this.peerRef.fileLogger.logConnectionTo(peerId);
        }

        System.out.println("Handshaked from client to " + peerId);
        requestSockets.put(peerId, newSocket);

        try {
            Handler handler = new Handler(peerId);
            handlers.put(peerId, handler);
            handler.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        for (Integer peerId : this.peerRef.peerConfig.keySet()) {
            if (this.peerRef.id.equals(peerId)) {
                break;
            }

            try {
                connectTo(peerId, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Handler extends Thread {
        private final Integer peerId;

        public Handler(Integer peerId) throws UnknownHostException, IOException {
            this.peerId = peerId;
        }

        public void run() {
            while (true) {
                try {
                    if (peerRef.pieceCount.compareTo(peerRef.totalPieces) >= 0) {
                        peerRef.hasFile = true;
                        sendInterest();
                        close(this.peerId);
                        peerRef.fileLogger.logDownloadedCompleteFile();
                        return;
                    }

                    InputStream input = requestSockets.get(this.peerId).getInputStream();
                    Message message = Message.decodeMessage(input);

                    switch (message.type()) {
                        case Message.Type.BITFIELD:
                            List<Boolean> bitfield = Message.decodeBitfield(message);
                            peerRef.neighborBitfields.put(peerId, bitfield);
                            sendInterest();
                            break;

                        case Message.Type.PIECE:
                            Message.PieceData pieceData = Message.decodePiece(message);
                            if (!peerRef.bitfield.get(pieceData.index())) {
                                BigInteger currentNeightborRate = peerRef.ratesFromNeighbors.get(peerId);
                                peerRef.ratesFromNeighbors.put(peerId,
                                        currentNeightborRate == null ? BigInteger.ONE
                                                : currentNeightborRate.add(BigInteger.ONE));

                                peerRef.fileMaker.writePiece(pieceData.index(), pieceData.fileData());
                                peerRef.bitfield.set(pieceData.index(), true);
                                peerRef.pieceCount++;

                                sendHave(pieceData.index());
                                peerRef.fileLogger.logDownloadedPiece(pieceData.index(), peerId,
                                        peerRef.pieceCount);

                                if (!unchokedBy.contains(peerId)) {
                                    sendRequest(peerId);
                                }
                            }
                            break;

                        case Message.Type.CHOKE:
                            unchokedBy.remove(peerId);
                            peerRef.fileLogger.logChokedBy(peerId);
                            break;

                        case Message.Type.UNCHOKE:
                            unchokedBy.add(peerId);
                            peerRef.fileLogger.logUnchokedBy(peerId);
                            sendRequest(peerId);
                            break;

                        default:
                            System.out.println(message.type());
                            break;
                    }
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    continue;
                } catch (SocketTimeoutException timeout) {
                    continue;
                } catch (EOFException eof) {
                    eof.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        requestSockets.get(this.peerId).close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    requestSockets.remove(peerId);
                }
            }
        }
    }

    public Map<Integer, Socket> getConnectedTo() {
        return this.requestSockets;
    }

    public void sendInterest() throws IOException {
        for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
            Socket connection = entry.getValue();
            Integer otherPeerId = entry.getKey();

            List<Boolean> otherBitfield = this.peerRef.neighborBitfields.get(otherPeerId);
            if (otherBitfield == null) {
                continue;
            }

            Boolean interested = false;
            for (int i = 0; i < this.peerRef.totalPieces; ++i) {
                if (!this.peerRef.bitfield.get(i) && otherBitfield.get(i)) {
                    interested = true;
                    break;
                }
            }

            connection.getOutputStream().write(Message.encodeInterest(interested));
        }
    }

    public void sendRequest(Integer otherPeerId) throws IOException {
        Socket connection = requestSockets.get(otherPeerId);
        List<Boolean> otherBitfield = this.peerRef.neighborBitfields.get(otherPeerId);
        List<Integer> otherPieceIndices = new ArrayList<>();

        for (int i = 0; i < otherBitfield.size(); ++i) {
            if (otherBitfield.get(i) && !this.peerRef.bitfield.get(i)) {
                otherPieceIndices.add(i);
            }
        }

        if (otherPieceIndices.isEmpty()) {
            return;
        }

        Random random = new Random();
        Integer pieceIndex = otherPieceIndices.get(random.nextInt(otherPieceIndices.size()));

        connection.getOutputStream().write(Message.encodeRequest(pieceIndex));
    }

    public void sendHave(Integer pieceIndex) throws IOException {
        for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
            Socket connection = entry.getValue();
            connection.getOutputStream().write(Message.encodeHave(pieceIndex));
        }
    }

    public void close(Integer peerId) throws IOException {
        handlers.get(peerId).interrupt();
        handlers.remove(peerId);
        requestSockets.get(peerId).close();
        requestSockets.remove(peerId);
    }
}
