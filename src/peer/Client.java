import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client extends Thread {
    private final Peer peerRef;

    private Map<Integer, Socket> requestSockets = new HashMap<>();
    private Map<Integer, List<Boolean>> neighbors = new HashMap<>();

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

                    switch (message.type()) {
                        case Message.Type.BITFIELD:
                            List<Boolean> bitfield = Message.decodeBitfield(message);
                            neighbors.put(entry.getKey(), bitfield);
                            System.out.println(bitfield);
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

}
