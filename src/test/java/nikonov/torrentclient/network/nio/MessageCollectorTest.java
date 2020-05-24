package nikonov.torrentclient.network.nio;

import nikonov.torrentclient.network.MessageCollector;
import nikonov.torrentclient.network.domain.message.HandshakeMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

@RunWith(MockitoJUnitRunner.class)
public class MessageCollectorTest {

    private MessageCollector collector = new MessageCollector();
    private byte[] handshake;
    private byte[] bitfield;
    private byte[] unchoke;
    private byte[] keepAlive;
    private byte[] piece;

    @Before
    public void setUp() {
        handshake = handshake();
        bitfield = message(100);
        unchoke = message(5);
        keepAlive = message(4);
        piece = message(250);

        collector.addBytes(handshake);
        var array = new byte[bitfield.length + unchoke.length + keepAlive.length + 100];
        System.arraycopy(bitfield, 0, array, 0, bitfield.length);
        System.arraycopy(unchoke, 0, array, bitfield.length, unchoke.length);
        System.arraycopy(keepAlive, 0, array, bitfield.length + unchoke.length, keepAlive.length);
        System.arraycopy(piece, 0, array, bitfield.length + unchoke.length + keepAlive.length, 100);
        collector.addBytes(array);
    }

    @Test
    public void messages() {
        var messages = collector.messages();
        Assert.assertEquals(4, messages.size());
        Assert.assertArrayEquals(handshake, messages.get(0));
        Assert.assertArrayEquals(bitfield, messages.get(1));
        Assert.assertArrayEquals(unchoke, messages.get(2));
        Assert.assertArrayEquals(keepAlive, messages.get(3));
        collector.addBytes(Arrays.copyOfRange(piece, 100, piece.length));
        messages = collector.messages();
        Assert.assertEquals(1, messages.size());
        Assert.assertArrayEquals(piece, messages.get(0));
    }

    private byte[] message(int size) {
        var buffer = ByteBuffer.allocate(size+4);
        buffer.putInt(size);
        var array = new byte[size];
        new Random().nextBytes(array);
        buffer.put(array);
        return buffer.array();
    }

    private byte[] handshake() {
        var array = new byte[HandshakeMessage.len()];
        new Random().nextBytes(array);
        return array;
    }
}
