import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
    NumberOfPreferredNeighbors 3
    UnchokingInterval 5
    OptimisticUnchokingInterval 10
    FileName thefile
    FileSize 2167705
    PieceSize 16384
 */
public record CommonConfigData(int numberOfPreferredNeighbors, int unchokingInterval, int optimisticUnchokingInterval,
        String fileName, int fileSize, int pieceSize) {

    private static final int COMMON_CONFIG_LEN = 6;

    public static CommonConfigData readInData(String configFileName) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(configFileName));

        if (lines.size() != COMMON_CONFIG_LEN) {
            throw new RuntimeException("Incorrect number of values in common config");
        }

        return new CommonConfigData(
                retrieveInt(lines, 0),
                retrieveInt(lines, 1),
                retrieveInt(lines, 2),
                retrieveString(lines, 3),
                retrieveInt(lines, 4),
                retrieveInt(lines, 5));
    }

    private static int retrieveInt(List<String> lines, int index) {
        return Integer.parseInt(lines.get(index).trim().split("\\s+")[1]);
    }

    private static String retrieveString(List<String> lines, int index) {
        return lines.get(index).trim().split("\\s+")[1];
    }

}
