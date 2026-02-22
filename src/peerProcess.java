import java.io.IOException;
import java.util.Map;

public class peerProcess {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("There must be a single arg passed of the peer process id (e.g. 1001)");
        }

        try {
            int peerProcessId = Integer.parseInt(args[0]);
            System.out.println("Starting Peer Process " + peerProcessId);

            CommonConfigData commonConfig = CommonConfigData.readInData("Common.cfg");

            Map<Integer, PeerConfigData> peerConfig = PeerConfigData.readInData("TestPeerInfo.cfg");

            FileLogger fileLogger = new FileLogger(peerProcessId);

            // Client: Initiates a connection, sends handshake, sends bitfield,
            // Server: Initiates a connection, sends handshake, sends bitfield,

            PeerConfigData peerData = peerConfig.get(peerProcessId);
            if (peerData == null) {
                throw new RuntimeException("No peer exists in the config for this peer process id");
            }

            new Peer(peerProcessId, peerData.hasFile(), commonConfig, peerConfig, fileLogger).run();
        } catch (NumberFormatException e) {
            throw new RuntimeException("Peer process id must be an integer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
