import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Length is in bytes
public record Message(Integer length, Type type, byte[] payload) {

    public static final int LENGTH_LEN = 4;
    public static final int TYPE_LEN = 1;

    /*
     * For message type checking. The fromCode method can be used to get the object
     * for a type from a message
     */
    public static enum Type {

        CHOKE((byte) 0),
        UNCHOKE((byte) 1),
        INTERESTED((byte) 2),
        NOT_INTERESTED((byte) 3),
        HAVE((byte) 4),
        BITFIELD((byte) 5),
        REQUEST((byte) 6),
        PIECE((byte) 7);

        private final byte code;
        private static final Map<Byte, Type> LOOKUP = new HashMap<>();

        static {
            for (Type t : values()) {
                LOOKUP.put(t.code, t);
            }
        }

        public static Type fromCode(byte code) throws IllegalArgumentException {
            Type t = LOOKUP.get(code);

            if (t == null) {
                throw new IllegalArgumentException("No message type for code " + code + " exists");
            }

            return t;
        }

        Type(byte code) {
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

    public static record PieceData(Integer index, byte[] fileData) {

        public static final int INDEX_LEN = 4;

    }

    public static PieceData decodePiece(Message message) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(message.payload());
        int index = byteBuffer.getInt();
        int remainingLength = byteBuffer.remaining();
        byte[] remainingBytes = new byte[remainingLength];
        System.out.println(remainingBytes.length);
        byteBuffer.get(remainingBytes);

        return new PieceData(index, remainingBytes);
    }

    public static byte[] encodePiece(PieceData pieceData, int pieceSize) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_LEN + TYPE_LEN + PieceData.INDEX_LEN + pieceSize);
        byteBuffer.putInt(PieceData.INDEX_LEN + pieceSize);
        byteBuffer.put(Type.PIECE.code);
        byteBuffer.putInt(pieceData.index());
        byteBuffer.put(pieceData.fileData());

        return byteBuffer.array();
    }

    /** Used to decode a handshake message and return a peerId */
    public static Integer decodeHandshake(InputStream inputStream) throws IOException, EOFException {
        inputStream.readNBytes(Handshake.HEADER_LEN);
        inputStream.readNBytes(Handshake.ZERO_LEN);
        byte[] peerId = inputStream.readNBytes(Handshake.PEER_LEN);
        return ByteBuffer.wrap(peerId).getInt();
    }

    /** Used to encode a handshaake message with the peerId */
    public static byte[] encodeHandshake(Integer peerId) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(Handshake.LEN);
        byteBuffer.put(Handshake.HEADER.getBytes());
        byteBuffer.put(new byte[Handshake.ZERO_LEN]);
        byteBuffer.putInt(peerId);

        return byteBuffer.array();
    }

    public static List<Boolean> decodeBitfield(Message message) {
        List<Boolean> bitfield = new ArrayList<>();

        for (byte b : message.payload()) {
            bitfield.add(b != 0);
        }

        return bitfield;
    }

    /** Decodes a message that contains a piece index field and return that index */
    public static Integer decodeIndexField(Message message) {
        return ByteBuffer.wrap(message.payload()).getInt();
    }

    public static Boolean decodeInterest(Message message) {
        return message.type.equals(Type.INTERESTED);
    }

    public static byte[] encodeInterest(Boolean interested) {
        Integer payloadSize = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_LEN + TYPE_LEN + payloadSize);
        byteBuffer.putInt(payloadSize);
        byteBuffer.put(interested ? Type.INTERESTED.code : Type.NOT_INTERESTED.code);

        return byteBuffer.array();
    }

    public static byte[] encodeChoke(Boolean choked) {
        Integer payloadSize = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_LEN + TYPE_LEN + payloadSize);
        byteBuffer.putInt(payloadSize);
        byteBuffer.put(choked ? Type.CHOKE.code : Type.UNCHOKE.code);

        return byteBuffer.array();
    }

    public static Message decodeMessage(InputStream inputStream)
            throws IllegalArgumentException, IOException, BufferUnderflowException {
        Integer length = ByteBuffer.wrap(inputStream.readNBytes(LENGTH_LEN)).getInt();
        Type type = Type.fromCode(ByteBuffer.wrap(inputStream.readNBytes(TYPE_LEN)).get());
        byte[] payload = inputStream.readNBytes(length);
        // System.out.println(length + " " + type);
        return new Message(length, type, payload);
    }

    public static byte[] encodeBitfield(List<Boolean> bitfield) {
        Integer payloadSize = bitfield.size();
        ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_LEN + TYPE_LEN + payloadSize);
        byteBuffer.putInt(payloadSize);
        byteBuffer.put(Type.BITFIELD.code);

        for (Boolean b : bitfield) {
            byteBuffer.put((byte) (b ? 1 : 0));
        }

        return byteBuffer.array();
    }

    public static byte[] encodeRequest(Integer pieceIndex) {
        Integer payloadSize = 4;
        ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_LEN + TYPE_LEN + payloadSize);
        byteBuffer.putInt(payloadSize);
        byteBuffer.put(Type.REQUEST.code);
        byteBuffer.putInt(pieceIndex);

        return byteBuffer.array();
    }

    public static byte[] encodeHave(Integer pieceIndex) {
        Integer payloadSize = 4;
        ByteBuffer byteBuffer = ByteBuffer.allocate(LENGTH_LEN + TYPE_LEN + payloadSize);
        byteBuffer.putInt(payloadSize);
        byteBuffer.put(Type.HAVE.code);
        byteBuffer.putInt(pieceIndex);

        return byteBuffer.array();
    }

}
