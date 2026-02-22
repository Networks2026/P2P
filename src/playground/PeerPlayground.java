import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contains and bridges between the server and client.
 */
public class PeerPlayground {
  public final Integer id;
  public Boolean hasFile;
  public List<Boolean> file;

  protected final CommonConfigData commonConfig;
  protected final Map<Integer, PeerConfigData> peerConfig;

  private Client client;
  private Server server;
  public Object fileLogger;

  /**
   * Constructor
   * 
   * Setup peer information.
   * 
   * @throws IOException
   */
  public Peer(Integer id, Boolean hasFile, CommonConfigData commonConfig, Map<Integer, PeerConfigData> peerConfig)
      throws IOException {
    this.id = id;
    this.hasFile = hasFile;
    this.file = new ArrayList<>(); // TODO: Read file

    this.commonConfig = commonConfig;
    this.peerConfig = peerConfig;

    this.client = new Client(this);
    this.server = new Server(this);
  }

  /**
   * Starts server and client
   */
  void run() throws IOException {
    if (hasFile) {
      this.server.run();
    } else {
      this.server.run();
      this.client.run();
    }
  }

  /**
   * Records a new connection from another peer's client to our server,
   * our client will then try to make a connection to their server.
   * This allows our client to make connections to all other peers with
   * an id greater than ours.
   */
  public void recordNewServerConnection(Integer peerId) throws UnknownHostException, IOException {
    this.client.createNewConnection(peerId);
  }
}
