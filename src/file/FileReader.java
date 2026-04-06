import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileReader {

    private final RandomAccessFile fileCon;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public final Integer pieceSize;
    public final Integer fileSize;
    public final Integer pieceAmt;

    public FileReader(String path, Integer pieceSize, Integer fileSize) throws FileNotFoundException {
        try {
            this.fileCon = new RandomAccessFile(path, "r");
        } catch (FileNotFoundException e) {
            // Log the error the user made
            System.err.println("Error reading file");
            throw e;
        }
        this.pieceSize = pieceSize;
        this.fileSize = fileSize;
        this.pieceAmt = (int) Math.ceilDiv(fileSize, pieceSize);
    }

    /**
     *
     * Gets pieces indexed from zero
     *
     * @param pieceNum
     * @return
     * @throws IOException
     */
    public byte[] getPiece(int pieceNum) throws IOException {
        // Don't attempt to read
        // Subtract one from piece amt since first piece is 0 and not 1
        if (pieceNum > pieceAmt - 1 || pieceNum < 0) {
            throw new RuntimeException("Unexpected pieceNum: " + pieceNum);
        }

        byte[] pieceArray = new byte[this.pieceSize];

        lock.readLock().lock();
        try {
            // Get offset and set it
            long offset = (long) this.pieceSize * pieceNum;
            fileCon.seek(offset);
            boolean lastPiece = pieceNum == this.pieceAmt - 1;
            int pieceWriteSize = lastPiece ? (this.fileSize % this.pieceSize) : this.pieceSize;

            // Read the file given the offset and pieceSize
            try {
                fileCon.read(pieceArray, 0, pieceWriteSize);
            } catch (IOException ex) {
                System.err.println("Error reading file piece");
            }
        } finally {
            lock.readLock().unlock();
        }

        return pieceArray;
    }

    public void closeConnection() throws IOException {
        fileCon.close();
    }
}