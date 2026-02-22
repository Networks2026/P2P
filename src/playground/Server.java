import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    int clientIndex = 1;
    
    try {
      while (true) {
        // Receive new connection when needed
        new Handler(listener.accept(), clientIndex).start(); // TODO: Log connection
        
        // Tell peer about connection
        this.peerRef.recordNewServerConnection(clientIndex);
        
        // Increment index
        clientIndex++;
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
    private int clientIndex;
    
    public Handler(Socket connection, int clientIndex) {
      this.connection = connection;
      this.clientIndex = clientIndex;
    }
    
    /**
     * Handles connection from other peer's client,
     * called by Thread.start()
     */
    public void run() {}
    public void sendMessage(String message) {}
  }
}
