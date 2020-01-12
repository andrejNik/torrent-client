package nikonov.torrentclient.network.nio;

import nikonov.torrentclient.network.domain.message.HandshakeMessage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCollector {

    private final List<byte[]> bufferList;
    private final List<byte[]> messages;
    private boolean handshake;

    public MessageCollector() {
        bufferList = new LinkedList<>();
        messages = new ArrayList<>();
        handshake = true;
    }

    public void addBytes(byte[] array) {
        bufferList.add(array);
        collect();
    }

    public List<byte[]> messages() {
        var list = new ArrayList<>(messages);
        messages.clear();
        return list;
    }

    private void collect() {
        var buffer = byteBuffer();
        bufferList.clear();
        if (handshake && buffer.capacity() >= HandshakeMessage.len()) {
            messages.add(collectHandshake(buffer));
            handshake = false;
        }
        if (!handshake) {
            messages.addAll(collectMessages(buffer));
        }
        bufferList.add(0, arrayFromBuffer(buffer));
    }

    private byte[] collectHandshake(ByteBuffer buffer) {
        var handshakeArray = new byte[HandshakeMessage.len()];
        buffer.get(handshakeArray);
        return handshakeArray;
    }

    private List<byte[]> collectMessages(ByteBuffer buffer) {
        var list = new ArrayList<byte[]>();
        while(buffer.capacity() - buffer.position() > 4) {
            buffer.mark();
            var size = buffer.getInt();
            if (buffer.capacity() - buffer.position() >= size) {
                buffer.reset();
                var messageArray = new byte[size + 4];
                buffer.get(messageArray);
                list.add(messageArray);
            } else {
                buffer.reset();
                break;
            }
        }
        return list;
    }

    private byte[] arrayFromBuffer(ByteBuffer buffer) {
        var array = new byte[buffer.limit() - buffer.position()];
        buffer.get(array);
        return array;
    }

    private ByteBuffer byteBuffer() {
        var size = bufferList.stream().mapToInt(array -> array.length).sum();
        var buff = ByteBuffer.allocate(size);
        bufferList.forEach(buff::put);
        buff.flip();
        return buff;
    }
}
