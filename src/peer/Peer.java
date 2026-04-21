import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Contains and bridges between the server and client.
 */
public class Peer {

    public final Integer id;
    public Boolean hasFile;
    public List<Boolean> bitfield;
    public Integer totalPieces;
    public Integer pieceCount;
    public ConcurrentHashMap<Integer, List<Boolean>> neighborBitfields = new ConcurrentHashMap<>();
    public Set<Integer> neighborsInterested = ConcurrentHashMap.newKeySet();

    /**
     * For tracking who is sending the most data to this peer. For determining
     * who should be unchoked or choked during the next interval. Unchoking is
     * also based on expressed interest though.
     */
    public ConcurrentHashMap<Integer, BigInteger> ratesFromNeighbors = new ConcurrentHashMap<>();

    protected final CommonConfigData commonConfig;
    protected final Map<Integer, PeerConfigData> peerConfig;
    protected final FileLogger fileLogger;
    protected final FileMaker fileMaker;
    protected final FileReader fileReader;

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
        this.bitfield = Collections
                .synchronizedList(new ArrayList<>(Collections.nCopies(this.totalPieces, this.hasFile)));

        String filePath = FileMaker.createDirAndFile(this.id, this.commonConfig.fileName(), this.hasFile);
        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.fileMaker = new FileMaker(filePath, this.commonConfig.pieceSize(), this.commonConfig.fileSize(), lock);
        this.fileReader = new FileReader(filePath, this.commonConfig.pieceSize(), this.commonConfig.fileSize(), lock);

        this.client = new Client(this);
        this.server = new Server(this);
    }

    /**
     * Starts server and client
     */
    void run() throws IOException {
        this.server.start();
        this.client.start();
    }

    /**
     * Records a new connection from another peer's client to our server, our
     * client will then try to make a connection to their server. This allows
     * our client to make connections to all other peers with an id greater than
     * ours.
     *
     * No need to log when it is matching a new client connection
     */
    public void recordNewServerConnection(Integer peerId, Boolean noLog) throws UnknownHostException, IOException {
        if (!this.hasFile) {
            this.client.connectTo(peerId, noLog);
        }
    }

    /**
     * Determines if client is interested based on new information that another
     * peer has a new piece. As long as this peer does not have the file, the
     * client will express interest if it needs the new piece.
     *
     * @throws IOException
     */
    public void recordHave() throws IOException {
        if (!this.hasFile) {
            this.client.sendInterest();
        }
    }
}
