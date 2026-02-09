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
    public enum Type {

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

        public static final int HEADER_LEN = 18;
        public static final int ZERO_LEN = 10;
        public static final int PEER_LEN = 4;

    }

}
