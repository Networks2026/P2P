import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        newSocket.setSoTimeout(200);

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
        requestSockets.put(peerId, newSocket);
    }

    public void run() {
        for (Integer peerId : this.peerRef.peerConfig.keySet()) {
            if (this.peerRef.id.equals(peerId)) {
                break;
            }

            try {
                this.connectTo(peerId, false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        while (!requestSockets.isEmpty()) {
            for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
                try {
                    InputStream input = entry.getValue().getInputStream();
                    Message message = Message.decodeMessage(input);
                    Integer otherPeerId = entry.getKey();

                    switch (message.type()) {
                        case Message.Type.BITFIELD:
                            List<Boolean> bitfield = Message.decodeBitfield(message);
                            this.peerRef.neighborBitfields.put(otherPeerId, bitfield);
                            // System.out.println(bitfield);
                            this.sendInterest();
                            break;

                        case Message.Type.PIECE:
                            Message.PieceData pieceData = Message.decodePiece(message);
                            if (!this.peerRef.bitfield.get(pieceData.index())) {
                                // System.out.println(pieceData.index());
                                this.peerRef.bitfield.set(pieceData.index(), true);
                                this.peerRef.pieceCount++;
                                this.peerRef.fileMaker.writePiece(pieceData.index(), pieceData.fileData());
                                this.sendHave(pieceData.index());

                                this.peerRef.fileLogger.logDownloadedPiece(pieceData.index(), otherPeerId,
                                        this.peerRef.pieceCount);

                                if (this.peerRef.pieceCount.equals(this.peerRef.totalPieces)) {
                                    this.peerRef.hasFile = true;
                                    this.sendInterest();
                                    this.close();
                                    this.peerRef.fileLogger.logDownloadedCompleteFile();
                                    return;
                                }

                                if (!unchokedBy.contains(otherPeerId)) {
                                    this.sendRequest(otherPeerId);
                                }
                            }
                            break;

                        case Message.Type.CHOKE:
                            unchokedBy.remove(otherPeerId);
                            this.peerRef.fileLogger.logChokedBy(otherPeerId);
                            break;

                        case Message.Type.UNCHOKE:
                            unchokedBy.add(otherPeerId);
                            this.peerRef.fileLogger.logUnchokedBy(otherPeerId);
                            this.sendRequest(otherPeerId);
                            break;

                        default:
                            System.out.println(message.type());
                            break;
                    }
                } catch (SocketTimeoutException timeout) {
                    continue;
                } catch (EOFException eof) {
                    eof.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        entry.getValue().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    requestSockets.remove(entry.getKey());
                }
            }
        }
        System.out.println("Client has no more remaining connections");
    }

    public void close() {
        for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        requestSockets.clear();
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
}
