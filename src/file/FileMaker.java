import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReadWriteLock;

public class FileMaker {

    private final FileChannel fileCon;
    private final ReadWriteLock lock;

    public final Integer pieceSize;
    public final Integer fileSize;
    public final Integer pieceAmt;

    public FileMaker(String path, Integer pieceSize, Integer fileSize, ReadWriteLock lock)
            throws IOException {

        try {
            this.fileCon = FileChannel.open(Path.of(path), StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (FileNotFoundException e) {
            System.err.println("Error opening file");
            throw e;
        }

        this.pieceSize = pieceSize;
        this.fileSize = fileSize;
        this.pieceAmt = Math.ceilDiv(fileSize, pieceSize);
        this.lock = lock;
    }

    public void writePiece(int pieceNum, byte[] pieceArray) throws IOException {
        if (pieceNum < 0 || pieceNum >= pieceAmt) {
            throw new RuntimeException("Unexpected pieceNum: " + pieceNum);
        }

        int bytesToWrite = getPieceLength(pieceNum);

        if (pieceArray.length != pieceSize && pieceArray.length != bytesToWrite) {
            throw new RuntimeException("Unexpected pieceArray size: " + pieceArray.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(pieceArray, 0, bytesToWrite);
        long offset = getPieceOffset(pieceNum);

        lock.writeLock().lock();
        try {
            while (buffer.hasRemaining()) {
                fileCon.write(buffer, offset + buffer.position());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

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
