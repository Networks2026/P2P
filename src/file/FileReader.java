import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileReader {

    private final RandomAccessFile fileCon;
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
        this.pieceAmt = (int) Math.ceil((float) fileSize / pieceSize);
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

        // Subtracts one since one-indexed
        // pieceNum = pieceNum - 1;

        // Don't attempt to read
        // Subtract one from piece amt since first piece is 0 and not 1
        if (pieceNum > pieceAmt - 1 || pieceNum < 0) {
            return null;
        }

        byte[] pieceArray = new byte[this.pieceSize];

        // Get offset and set it
        long offset = (long) this.pieceSize * pieceNum;
        fileCon.seek(offset);

        // Read the file given the offset and pieceSize

        try {
            fileCon.read(pieceArray, 0, this.pieceSize);
        } catch (IOException ex) {
            System.err.println("Error reading file piece");
        }

        return pieceArray;
    }

    public void closeConnection() throws IOException {
        fileCon.close();
    }
}