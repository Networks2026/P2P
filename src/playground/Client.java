import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for initiating a connection to 
 * another server, and receives file data from the connected server.
 */
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
  
  /**
   * Connects to all servers before it
   * @throws IOException 
   * @throws UnknownHostException 
   */
  void run() throws UnknownHostException, IOException {
    
    List<Integer> peers = new ArrayList<>(); // TODO: Read config
    
    // For each previous peer, create new connection
    for (Integer peerId : peers) {
      this.createNewConnection(peerId);
    }
  }
  
  /**
   * Initialize new connection to another peer's server.
   */
  public void createNewConnection(Integer peerId) throws UnknownHostException, IOException {
    Integer port = 8000; // TODO: Read config
    String host = "localhost"; // TODO: Read config
    
    Socket socket = new Socket(host, port);
    this.requestSockets.put(peerId, socket);
  }
  
  void sendMessage(String message) {}
}
