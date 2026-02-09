import java.io.IOException;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class peerProcess {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("There must be a single arg passed of the peer process id (e.g. 1001)");
        }

        try {
            int peerProcessId = Integer.parseInt(args[0]);
            System.out.println("Starting Peer Process " + peerProcessId);

            CommonConfig commonConfig = CommonConfig.readInData("Common.cfg");
            System.out.println(commonConfig);

            Map<Integer, PeerConfigData> peerConfig = PeerConfigData.readInData("PeerInfo.cfg");
            System.out.println(peerConfig);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Peer process id must be an integer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
