package nikonov.torrentclient.network;

import nikonov.torrentclient.domain.Bitfield;
import nikonov.torrentclient.network.domain.message.*;

import java.nio.ByteBuffer;

import static nikonov.torrentclient.network.domain.message.HandshakeMessage.HANDSHAKE_PSTR;
import static nikonov.torrentclient.network.domain.message.HandshakeMessage.HANDSHAKE_PSTRLEN;


public class MessageEncoder {

    public <T extends Message> byte[] encode(T message) {
        if (message instanceof HandshakeMessage) {
            return encodeHandshake((HandshakeMessage) message);
        }
        if (message instanceof BitfieldMessage) {
            return encodeBitfield((BitfieldMessage) message);
        }
        if (message instanceof CancelMessage) {
            throw new UnsupportedOperationException();
        }
        if (message instanceof ChokeMessage) {
            return encodeChoke();
        }
        if (message instanceof HaveMessage) {
            return encodeHave((HaveMessage) message);
        }
        if (message instanceof InterestedMessage) {
            return encodeInterested();
        }
        if (message instanceof KeepAliveMessage) {
            return encodeKeepAlive();
        }
        if (message instanceof NotInterestedMessage) {
            return encodeNotInterested();
        }
        if (message instanceof PieceMessage) {
            return encodePiece((PieceMessage) message);
        }
        if (message instanceof PortMessage) {
            throw new UnsupportedOperationException();
        }
        if (message instanceof RequestMessage) {
            return encodeRequest((RequestMessage) message);
        }
        if (message instanceof UnchokeMessage) {
            return encodeUnchoke();
        }
        throw new IllegalArgumentException("неизвестный тип сообщения");
    }

    private byte[] encodeUnchoke() {
        return arrayFromBuffer(ByteBuffer.allocate(UnchokeMessage.len()).putInt(1).put(InterestedMessage.id()));
    }

    private byte[] encodeRequest(RequestMessage message) {
        return arrayFromBuffer(ByteBuffer
                .allocate(RequestMessage.len())
                .putInt(13)
                .put(RequestMessage.id())
                .putInt(message.getIndex())
                .putInt(message.getBegin())
                .putInt(message.getLength()));
    }

    private byte[] encodePiece(PieceMessage message) {
        return arrayFromBuffer(ByteBuffer
                .allocate(13 + message.getBlock().length)
                .putInt(9 + message.getBlock().length)
                .put(PieceMessage.id())
                .putInt(message.getIndex())
                .putInt(message.getBegin())
                .put(message.getBlock()));
    }

    private byte[] encodeNotInterested() {
        return arrayFromBuffer(ByteBuffer.allocate(NotInterestedMessage.len()).putInt(1).put(NotInterestedMessage.id()));
    }

    private byte[] encodeKeepAlive() {
        return arrayFromBuffer(ByteBuffer.allocate(KeepAliveMessage.len()).putInt(0));
    }

    private byte[] encodeInterested() {
        return arrayFromBuffer(ByteBuffer.allocate(InterestedMessage.len()).putInt(1).put(InterestedMessage.id()));
    }

    private byte[] encodeHave(HaveMessage message) {
        return arrayFromBuffer(ByteBuffer
                .allocate(HaveMessage.len())
                .putInt(5)
                .put(HaveMessage.id())
                .putInt(message.getPieceIndex()));
    }

    private byte[] encodeChoke() {
        return arrayFromBuffer(ByteBuffer
                .allocate(ChokeMessage.len())
                .putInt(1)
                .put(ChokeMessage.id()));
    }

    private byte[] encodeBitfield(BitfieldMessage message) {
        var bitfield = bitfield(message.getBitfield());
        return arrayFromBuffer(ByteBuffer
                .allocate(5 + bitfield.length)
                .putInt(1 + bitfield.length)
                .put(BitfieldMessage.id())
                .put(bitfield));
    }

    private byte[] bitfield(Bitfield bitfield) {
        var currentIndex = 0;
        var maxIndex = bitfield.size() - 1;
        var array = new byte[bitfield.size() / 8];
        while (currentIndex < maxIndex) {
            var item = 0;
            for (var i = 0; i < 8; i++) {
                if (bitfield.isHave(currentIndex)) {
                    item += Math.pow(2, 7 - i);
                }
                currentIndex++;
            }
            array[(currentIndex - 1) / 8] = (byte)item;
        }
        return array;
    }

    private byte[] encodeHandshake(HandshakeMessage message) {
        return arrayFromBuffer(ByteBuffer
                .allocate(HandshakeMessage.len())
                .put(HANDSHAKE_PSTRLEN)
                .put(HANDSHAKE_PSTR.getBytes())
                .putLong(0)
                .put(message.getInfoHash())
                .put(message.getPeerId()));
    }

    private byte[] arrayFromBuffer(ByteBuffer buffer) {
        var array = new byte[buffer.capacity()];
        buffer.flip();
        buffer.get(array);
        return array;
    }
}
