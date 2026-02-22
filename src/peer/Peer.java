import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Contains and bridges between the server and client.
 */
public class Peer {
    public final Integer id;
    public Boolean hasFile;
    public List<Boolean> bitfield;
    public Integer totalPieces;
    public Integer pieceCount;

    protected final CommonConfigData commonConfig;
    protected final Map<Integer, PeerConfigData> peerConfig;
    protected final FileLogger fileLogger;

    protected Client client;
    private Server server;

    /**
     * Constructor
     * 
     * Setup peer information.
     * 
     * @throws IOException
     */
    public Peer(Integer id, Boolean hasFile, CommonConfigData commonConfig, Map<Integer, PeerConfigData> peerConfig,
            FileLogger fileLogger)
            throws IOException {
        this.id = id;
        this.hasFile = hasFile;
        this.commonConfig = commonConfig;
        this.peerConfig = peerConfig;
        this.fileLogger = fileLogger;

        this.totalPieces = Math.ceilDiv(this.commonConfig.fileSize(), this.commonConfig.pieceSize());
        this.pieceCount = this.hasFile ? this.totalPieces : 0;
        this.bitfield = new ArrayList<>(Collections.nCopies(this.totalPieces, this.hasFile));

        this.client = new Client(this);
        this.server = new Server(this);
    }

    /**
     * Starts server and client
     */
    void run() throws IOException {
        if (this.hasFile) {
            this.server.start();
        } else {
            this.client.start();
            this.server.start();
        }
    }

    /**
     * Records a new connection from another peer's client to our server,
     * our client will then try to make a connection to their server.
     * This allows our client to make connections to all other peers with
     * an id greater than ours.
     * 
     * No need to log when it is matching a new client connection
     */
    public void recordNewServerConnection(Integer peerId, Boolean noLog) throws UnknownHostException, IOException {
        if (!this.hasFile) {
            this.client.connectTo(peerId, noLog);
        }
    }
}
