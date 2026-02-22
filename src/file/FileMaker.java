import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileMaker {

    private final RandomAccessFile fileCon;

    public final Integer pieceSize;
    public final Integer fileSize;
    public final Integer pieceAmt;
    private final Boolean hasPad;


    public FileMaker(String path, Integer pieceSize, Integer fileSize) throws FileNotFoundException {

        try {
            //Makes file if it doesnt exist - which it usually doesn't for a new client
            this.fileCon = new RandomAccessFile (path, "rw");
        }catch (FileNotFoundException e){
            //Log the error the user made
            System.err.println("Error opening file");
            throw e;
        }

        this.pieceSize = pieceSize;
        this.fileSize = fileSize;
        this.pieceAmt = fileSize / pieceSize;
        this.hasPad = fileSize % pieceSize != 0;
    }

    /**
     * Piece num starts at 0 instead of 1
     * @param pieceNum
     * @param pieceArray
     * @throws IOException
     */
    public void writePiece(int pieceNum, byte[] pieceArray) throws IOException {

        if (pieceNum > pieceAmt || pieceNum < 0) {
            return;
        }

        if (pieceArray.length != this.pieceSize) {
            return;
        }

        boolean lastPiece = pieceNum == this.pieceAmt;
        if (lastPiece) {
            System.out.println(pieceNum);
        }


        long offset = (long) this.pieceSize * pieceNum;
        int pieceWriteSize = lastPiece ? (this.fileSize % this.pieceSize) : this.pieceSize;
        fileCon.seek(offset);

        fileCon.write(pieceArray, 0, pieceWriteSize);
    }


    public void closeConnection() throws IOException {
      fileCon.close();
    }

}