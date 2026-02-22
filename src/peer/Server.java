import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Responsible for accepting connection requests from clients,
 * and sending file data to other clients.
 */
public class Server extends Thread {
    private final int port;
    private final Peer peerRef;

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
    }

    /**
     * Spawns from the listening loop and
     * are responsible for dealing with a single client's requests.
     */
    private static class Handler extends Thread {
        private String messageReceived;
        private String messageSent;
        private Socket connection;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Integer peerId;
        private Peer peerRef;

        public Handler(Socket connection, Peer peerRef) throws IOException {
            this.connection = connection;
            this.peerRef = peerRef;

            connection.setSoTimeout(200);

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
                } catch (SocketTimeoutException timeout) {
                    continue;
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        this.connection.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void sendMessage(String message) {
        }
    }
}
