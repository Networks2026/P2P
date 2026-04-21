import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReadWriteLock;

public class FileReader {

    private final FileChannel fileCon;
    private final ReadWriteLock lock;

    public final Integer pieceSize;
    public final Integer fileSize;
    public final Integer pieceAmt;

    public FileReader(String path, Integer pieceSize, Integer fileSize, ReadWriteLock lock)
            throws IOException {
        try {
            this.fileCon = FileChannel.open(Path.of(path), StandardOpenOption.READ);
        } catch (FileNotFoundException e) {
            // Log the error the user made
            System.err.println("Error reading file");
            throw e;
        }
        this.pieceSize = pieceSize;
        this.fileSize = fileSize;
        this.pieceAmt = (int) Math.ceilDiv(fileSize, pieceSize);
        this.lock = lock;
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
        if (pieceNum < 0 || pieceNum >= pieceAmt) {
            throw new RuntimeException("Unexpected pieceNum: " + pieceNum);
        }

        int bytesToRead = getPieceLength(pieceNum);
        byte[] pieceArray = new byte[pieceSize];
        ByteBuffer buffer = ByteBuffer.wrap(pieceArray, 0, bytesToRead);
        long offset = getPieceOffset(pieceNum);

        lock.readLock().lock();
        try {
            while (buffer.hasRemaining()) {
                int bytesRead = fileCon.read(buffer, offset + buffer.position());

                if (bytesRead == -1) {
                    throw new IOException("Unexpected EOF while reading piece " + pieceNum);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return pieceArray;
    }

    private long getPieceOffset(int pieceNum) {
        return (long) pieceNum * pieceSize;
    }

    private int getPieceLength(int pieceNum) {
        long offset = getPieceOffset(pieceNum);
        return (int) Math.min(pieceSize, fileSize - offset);
    }

    public void closeConnection() throws IOException {
        fileCon.close();
    }
}
