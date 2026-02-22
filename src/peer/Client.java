import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client {
    private final Peer peerRef;

    private Socket requestSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String messageSent;
    private String messageReceived;

    private Map<Integer, Socket> requestSockets;
    private Map<String, List<Boolean>> neighbors;

    public Client(Peer peerRef) {
        this.peerRef = peerRef;
        this.neighbors = new HashMap<>();
    };

    public void connectTo(int peerId) throws UnknownHostException, IOException {
        PeerConfigData peerData = this.peerRef.peerConfig.get(peerId);
        Socket newSocket = new Socket(peerData.hostName(), peerData.port());
        this.peerRef.fileLogger.logConnectionTo(peerId);
        requestSockets.put(peerId, newSocket);
    }

    public void run() throws UnknownHostException, IOException {
        for (Integer peerId : this.peerRef.peerConfig.keySet()) {
            if (this.peerRef.id.equals(peerId)) {
                break;
            }
            this.connectTo(peerId);
        }

        while (true) {
            for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
                if (this.peerRef.hasFile) {
                    this.close();
                    return;
                }
                if (entry.getValue().isClosed()) {
                    continue;
                }
                try {
                    InputStream input = entry.getValue().getInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        entry.getValue().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void close() {
        for (Map.Entry<Integer, Socket> entry : requestSockets.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
