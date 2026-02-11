import java.io.IOException;
import java.util.List;
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

            FileLogger fileLogger = new FileLogger(peerProcessId);
            fileLogger.logConnectionTo(1002);
            fileLogger.logConnectionFrom(1010);
            fileLogger.logPreferredNeighbors(List.of(1002, 1003, 1004));
            fileLogger.logOptimisticallyUnchokedNeighbor(1005);
            fileLogger.logUnchokedBy(1002);
            fileLogger.logChokedBy(1003);
            fileLogger.logReceivedHave(1004, 7);
            fileLogger.logReceivedInterested(1005);
            fileLogger.logReceivedNotInterested(1006);
            fileLogger.logDownloadedPiece(7, 1004, 12);
            fileLogger.logDownloadedCompleteFile();

        } catch (NumberFormatException e) {
            throw new RuntimeException("Peer process id must be an integer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
