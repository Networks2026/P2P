import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Responsible for accepting connection requests from clients,
 * and sending file data to other clients.
 */
public class Server {
    private final int port;
    private final Peer peerRef;

    public Server(Peer peerRef) {
        this.peerRef = peerRef;
        this.port = this.peerRef.peerConfig.get(this.peerRef.id).port();
    }

    /**
     * Start server to accept incoming client connection requests
     */
    public void run() throws IOException {
        ServerSocket listener = new ServerSocket(this.port);
        System.out.println("Started listening on: " + this.port);

        try {
            while (true) {
                // Receive new connection when needed
                Handler handler = new Handler(listener.accept(), this.peerRef);
                handler.start();

                // Tell peer about connection
                this.peerRef.recordNewServerConnection(handler.peerId);
            }
        } finally {
            listener.close();
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

            InputStream inputStream = connection.getInputStream();
            while (true) {
                try {
                    this.peerId = Message.decodeHandshake(inputStream);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            this.peerRef.fileLogger.logConnectionFrom(this.peerId);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(Message.encodeHandshake(this.peerRef.id));
        }

        /**
         * Handles connection from other peer's client,
         * called by Thread.start()
         */
        public void run() {
            try {
                InputStream input = this.connection.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    this.connection.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
        }
    }
}
