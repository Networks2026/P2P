import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
    1001 lin114-00.cise.ufl.edu 6001 1 
    1002 lin114-01.cise.ufl.edu 6001 0 
    1003 lin114-02.cise.ufl.edu 6001 0 
    1004 lin114-03.cise.ufl.edu 6001 0 
    1005 lin114-04.cise.ufl.edu 6001 0 
    1006 lin114-05.cise.ufl.edu 6001 1
    1007 lin114-06.cise.ufl.edu 6001 0 
    1008 lin114-07.cise.ufl.edu 6001 0 
    1009 lin114-08.cise.ufl.edu 6001 0 
 */

public record PeerConfigData(int peerId, String hostName, int port, boolean hasFile) {

    public static Map<Integer, PeerConfigData> readInData(String configFileName) throws IOException {
        Map<Integer, PeerConfigData> peerConfigMap = new TreeMap<>();
        List<String> lines = Files.readAllLines(Path.of(configFileName));

        if (lines.size() == 0) {
            throw new RuntimeException("No peers found in the peer config");
        }

        for (String line : lines) {
            String[] lineData = line.trim().split("\\s+");
            if (lineData.length != 4) {
                throw new RuntimeException("A row in the peer config does not have the correct length: " + line);
            }

            Integer peerId = Integer.parseInt(lineData[0]);
            String hasFileText = lineData[3];
            boolean hasFile = switch (hasFileText) {
                case "0" -> false;
                case "1" -> true;
                default -> throw new IllegalArgumentException(
                        "Has file portion of config must either be a 0 or 1, received: " + hasFileText);
            };
            peerConfigMap.put(peerId, new PeerConfigData(peerId, lineData[1], Integer.parseInt(lineData[2]), hasFile));
        }

        return peerConfigMap;
    }

}
