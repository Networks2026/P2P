import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        FileReader fileReader = new FileReader("peer_1001/testfile", 100000, 2167705);
        FileMaker fileMaker = new FileMaker("file_maker_test", 100000, 2167705);

        List<Integer> unusedPieces = new ArrayList<>();
        IntStream.range(0, fileReader.pieceAmt).forEach(unusedPieces::add);
        Random random = new Random();
        Collections.shuffle(unusedPieces);

        for (int i = 0; i < fileReader.pieceAmt; ++i) {
            int index = random.nextInt(unusedPieces.size());
            fileMaker.writePiece(unusedPieces.get(index), fileReader.getPiece(unusedPieces.get(index)));
            unusedPieces.remove(index);
        }
    }
}
