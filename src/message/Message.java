import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

// Length is in bytes
public class Message {

    public static final int LENGTH_LEN = 4;
    public static final int TYPE_LEN = 1;

    /*
     * For message type checking. The fromCode method can be used to get the object
     * for a type from a message
     */
    public static enum Type {

        CHOKE(0),
        UNCHOKE(1),
        INTERESTED(2),
        NOT_INTERESTED(3),
        HAVE(4),
        BITFIELD(5),
        REQUEST(6),
        PIECE(7);

        private final int code;
        private static final Map<Integer, Type> LOOKUP = new HashMap<>();

        static {
            for (Type t : values()) {
                LOOKUP.put(t.code, t);
            }
        }

        public static Type fromCode(int code) throws IllegalArgumentException {
            Type t = LOOKUP.get(code);

            if (t == null) {
                throw new IllegalArgumentException("No message type for code " + code + " exists");
            }

            return t;
        }

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

    }

    // For handshake messages specifically
    public static class Handshake {

        public static final int LEN = 32;
        public static final int HEADER_LEN = 18;
        public static final int ZERO_LEN = 10;
        public static final int PEER_LEN = 4;
        public static final String HEADER = "P2PFILESHARINGPROJ";

    }

    // Used to decode a handshake message and return a peerId
    public static Integer decodeHandshake(InputStream inputStream) throws IOException, EOFException {
        byte[] header = inputStream.readNBytes(Handshake.HEADER_LEN);
        if (header.length == 0) {
            throw new EOFException("No handshake message delivered");
        }

        inputStream.readNBytes(Handshake.ZERO_LEN);
        byte[] peerId = inputStream.readNBytes(Handshake.PEER_LEN);
        return ByteBuffer.wrap(peerId).getInt();
    }

    // Used to encode a handshaake message with the peerId
    public static byte[] encodeHandshake(Integer peerId) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Handshake.LEN);
        byteBuffer.put(Handshake.HEADER.getBytes());
        byteBuffer.put(new byte[Handshake.ZERO_LEN]);
        byteBuffer.putInt(peerId);
        return byteBuffer.array();
    }

}
