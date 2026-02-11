import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public class FileLogger {

    private final int peerId;

    private final Path logFilePath;

    public FileLogger(int peerId) {
        this.peerId = peerId;
        this.logFilePath = Path.of("log_peer_" + this.peerId + ".log");
    }

    public void logConnectionTo(int otherPeerId) {
        writeToFile(getPeerString().append("makes a connection to Peer ").append(otherPeerId)
                .append("."));
    }

    public void logConnectionFrom(int otherPeerId) {
        writeToFile(getPeerString().append("is connected from Peer ").append(otherPeerId)
                .append("."));
    }

    public void logPreferredNeighbors(List<Integer> preferredNeighborIds) {
        StringBuilder sb = getPeerString().append("has the preferred neighbors ");
        for (int i = 0; i < preferredNeighborIds.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(preferredNeighborIds.get(i));
        }
        sb.append(".");
        writeToFile(sb);
    }

    public void logOptimisticallyUnchokedNeighbor(int optimUnchokedId) {
        writeToFile(getPeerString().append("has the optimistically unchoked neighbor ")
                .append(optimUnchokedId)
                .append("."));
    }

    public void logUnchokedBy(int otherPeerId) {
        writeToFile(getPeerString().append("is unchoked by ")
                .append(otherPeerId)
                .append("."));
    }

    public void logChokedBy(int otherPeerId) {
        writeToFile(getPeerString().append("is choked by ")
                .append(otherPeerId)
                .append("."));
    }

    public void logReceivedHave(int otherPeerId, int pieceIndex) {
        writeToFile(getPeerString().append("received the 'have' message from ")
                .append(otherPeerId)
                .append(" for the piece ")
                .append(pieceIndex)
                .append("."));
    }

    public void logReceivedInterested(int otherPeerId) {
        writeToFile(getPeerString().append("received the 'interested' message from ")
                .append(otherPeerId)
                .append("."));
    }

    public void logReceivedNotInterested(int otherPeerId) {
        writeToFile(getPeerString().append("received the 'not interested' message from ")
                .append(otherPeerId)
                .append("."));
    }

    public void logDownloadedPiece(int pieceIndex, int fromPeerId, int piecesHaveCount) {
        writeToFile(getPeerString().append("has downloaded the piece ")
                .append(pieceIndex)
                .append(" from ")
                .append(fromPeerId)
                .append(". Now the number of pieces it has is ")
                .append(piecesHaveCount)
                .append("."));
    }

    public void logDownloadedCompleteFile() {
        writeToFile(getPeerString().append("has downloaded the complete file."));
    }

    /**
     * Write a string to a file and adds a new line at the end of it. Appends if it
     * exists, or creates a new file.
     */
    private void writeToFile(StringBuilder text) {
        try {
            Files.writeString(this.logFilePath, text + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to file", e);
        }
    }

    /**
     * Returns this portion of the string as a StringBuilder '[Time]: Peer [peerId]
     * '
     */
    private StringBuilder getPeerString() {
        StringBuilder sb = new StringBuilder();
        sb.append(LocalDateTime.now().toString()).append(": Peer ").append(this.peerId).append(" ");
        return sb;
    }

}
