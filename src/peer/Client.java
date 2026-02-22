import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client extends Thread {
    private final Peer peerRef;

    private Socket requestSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String messageSent;
    private String messageReceived;

    private Map<Integer, Socket> requestSockets = new HashMap<>();
    private Map<String, List<Boolean>> neighbors = new HashMap<>();

    public Client(Peer peerRef) {
        this.peerRef = peerRef;
    };

    public void connectTo(int peerId) throws UnknownHostException, IOException {
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

        this.peerRef.fileLogger.logConnectionTo(peerId);
        requestSockets.put(peerId, newSocket);
    }

    public void run() {
        System.out.println("Attempting to connect to previous neighbors");
        for (Integer peerId : this.peerRef.peerConfig.keySet()) {
            if (this.peerRef.id.equals(peerId)) {
                break;
            }

            try {
                this.connectTo(peerId);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        while (!requestSockets.isEmpty()) {
            for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
                try {
                    InputStream input = entry.getValue().getInputStream();
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

}
