import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.concurrent.locks.ReadWriteLock;

public class FileMaker {

    private final RandomAccessFile fileCon;
    private final ReadWriteLock lock;

    public final Integer pieceSize;
    public final Integer fileSize;
    public final Integer pieceAmt;

    public FileMaker(String path, Integer pieceSize, Integer fileSize, ReadWriteLock lock)
            throws FileNotFoundException {

        try {
            // Makes file if it doesnt exist - which it usually doesn't for a new client
            this.fileCon = new RandomAccessFile(path, "rw");
        } catch (FileNotFoundException e) {
            // Log the error the user made
            System.err.println("Error opening file");
            throw e;
        }

        this.pieceSize = pieceSize;
        this.fileSize = fileSize;
        this.pieceAmt = Math.ceilDiv(fileSize, pieceSize);
        this.lock = lock;
    }

    /**
     * Piece num starts at 0 instead of 1
     * 
     * @param pieceNum
     * @param pieceArray
     * @throws IOException
     */
    public void writePiece(int pieceNum, byte[] pieceArray) throws IOException {

        if (pieceNum > pieceAmt - 1 || pieceNum < 0) {
            throw new RuntimeException("Unexpected pieceNum: " + pieceNum);
        }

        if (pieceArray.length != this.pieceSize) {
            throw new RuntimeException("Unexpected pieceArray size: " + pieceArray.length);
        }

        lock.writeLock().lock();
        try {
            boolean lastPiece = pieceNum == this.pieceAmt - 1;
            long offset = (long) this.pieceSize * pieceNum;
            int pieceWriteSize = lastPiece ? (this.fileSize % this.pieceSize) : this.pieceSize;
            fileCon.seek(offset);
            fileCon.write(pieceArray, 0, pieceWriteSize);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void closeConnection() throws IOException {
        fileCon.close();
    }

    public static String createDirAndFile(Integer peerId, String fileName, Boolean hasFile) throws IOException {
        File dir = new File("peer_" + peerId);
        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = new File(dir, fileName);
        if (!hasFile) {
            Files.deleteIfExists(file.toPath());
            file.createNewFile();
        }

        return file.getPath();
    }
}