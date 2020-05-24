package nikonov.torrentclient.network.nio;

import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.event.EventService;
import nikonov.torrentclient.network.MessageCollector;
import nikonov.torrentclient.network.MessageDecoder;
import nikonov.torrentclient.network.MessageEncoder;
import nikonov.torrentclient.network.domain.event.DisconnectEvent;
import nikonov.torrentclient.network.domain.event.MessageEvent;
import nikonov.torrentclient.network.domain.message.Message;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkChannel {

    public static final int READ_BUFFER_SIZE = (int)Math.pow(2, 14) + 13;

    private final Queue<Message> messageQueue;
    private final SocketChannel socketChannel;
    private final EventService eventService;
    private final MessageDecoder messageDecoder;
    private final MessageEncoder messageEncoder;
    private final MessageCollector messageCollector;
    private final PeerAddress peerAddress;

    private ByteBuffer writeBuffer;
    private ByteBuffer readBuffer;

    public NetworkChannel(SocketChannel socketChannel, EventService eventService, PeerAddress peerAddress) {
        this.socketChannel = socketChannel;
        this.eventService = eventService;
        this.peerAddress = peerAddress;
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.messageDecoder = new MessageDecoder();
        this.messageEncoder = new MessageEncoder();
        this.messageCollector = new MessageCollector();
        this.readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.wrap(new byte[0]);
    }

    public void send(Message message) {
        messageQueue.add(message);
    }

    public void read() {
        try {
            var count = 0;
            do {
                count = socketChannel.read(readBuffer);
                readBuffer.flip();
                if (count > 0) {
                    var array = new byte[readBuffer.limit()];
                    readBuffer.get(array);
                    messageCollector.addBytes(array);
                }
                readBuffer.clear();
            } while (count > 0);
            for(var messageArray : messageCollector.messages()) {
                var message = messageDecoder.decode(messageArray);
                message.setSender(peerAddress);
                eventService.publishEvent(new MessageEvent(peerAddress, message));
            }
            if (count == -1) {
                closeAndPublish();
            }
        } catch (Throwable ignore) {
            // TODO логирование
            closeAndPublish();
        }
    }

    public void write() {
        try {
            if (writeBuffer.hasRemaining() || !messageQueue.isEmpty()) {
                if (!writeBuffer.hasRemaining()) {
                    writeBuffer = ByteBuffer.wrap(messageEncoder.encode(messageQueue.poll()));
                }
                socketChannel.write(writeBuffer);
            }
        } catch (Throwable ignore) {
            // TODO логирование
            closeAndPublish();
        }
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Throwable exp) {
            throw new RuntimeException(exp);
        }
    }

    private void closeAndPublish() {
        close();
        eventService.publishEvent(new DisconnectEvent(peerAddress));
    }
}
