package nikonov.torrentclient.client.network;

import nikonov.torrentclient.client.domain.Bitfield;
import nikonov.torrentclient.client.network.domain.message.*;

import java.nio.ByteBuffer;

public class MessageDecoder {

    private static final int ID_INDEX = 4;
    private static final int DATA_INDEX = 5;

    public Message decode(byte[] array) {
        var buffer = ByteBuffer.wrap(array);
        if (isHandshake(buffer)) {
            return decodeHandshake(buffer);
        }
        if (isBitfield(buffer)) {
            return decodeBitfield(buffer);
        }
        if (isCancel(buffer)) {
            return decodeCancel(buffer);
        }
        if (isChoke(buffer)) {
            return new ChokeMessage();
        }
        if (isHave(buffer)) {
            return decodeHave(buffer);
        }
        if (isInterested(buffer)) {
            return new InterestedMessage();
        }
        if (isKeepAlive(buffer)) {
            return new KeepAliveMessage();
        }
        if (isNotInterested(buffer)) {
            return new NotInterestedMessage();
        }
        if (isPiece(buffer)) {
            return decodePiece(buffer);
        }
        if (isPort(buffer)) {
            return decodePort(buffer);
        }
        if (isRequest(buffer)) {
            return decodeRequest(buffer);
        }
        if (isUnchoke(buffer)) {
            return new UnchokeMessage();
        }
        throw new IllegalArgumentException("неккоректное сообщение");
    }

    private boolean isUnchoke(ByteBuffer buffer) {
        return buffer.capacity() == UnchokeMessage.len() && buffer.get(ID_INDEX) == UnchokeMessage.id();
    }

    private boolean isRequest(ByteBuffer buffer) {
        return buffer.capacity() == RequestMessage.len() && buffer.get(ID_INDEX) == RequestMessage.id();
    }

    private RequestMessage decodeRequest(ByteBuffer buffer) {
        buffer.position(DATA_INDEX);
        var message = new RequestMessage();
        message.setIndex(buffer.getInt());
        message.setBegin(buffer.getInt());
        message.setLength(buffer.getInt());
        return message;
    }

    private boolean isPort(ByteBuffer buffer) {
        return buffer.capacity() == PortMessage.len() && buffer.get(ID_INDEX) == PortMessage.id();
    }

    private PortMessage decodePort(ByteBuffer buffer) {
        buffer.position(DATA_INDEX);
        var message = new PortMessage();
        message.setListenPort(buffer.getInt());
        return message;
    }

    private boolean isPiece(ByteBuffer buffer) {
        return buffer.capacity() > 12 && buffer.get(ID_INDEX) == PieceMessage.id();
    }

    private PieceMessage decodePiece(ByteBuffer buffer) {
        var message = new PieceMessage();
        buffer.position(DATA_INDEX);
        message.setIndex(buffer.getInt());
        message.setBegin(buffer.getInt());
        var block = new byte[buffer.array().length - 13];
        buffer.get(block);
        message.setBlock(block);
        return message;
    }

    private boolean isNotInterested(ByteBuffer buffer) {
        return buffer.capacity() == NotInterestedMessage.len() && buffer.get(ID_INDEX) == NotInterestedMessage.id();
    }

    private boolean isKeepAlive(ByteBuffer buffer) {
        return buffer.capacity() == KeepAliveMessage.len();
    }

    private boolean isInterested(ByteBuffer buffer) {
        return buffer.capacity() == InterestedMessage.len() && buffer.get(ID_INDEX) == InterestedMessage.id();
    }

    private boolean isHave(ByteBuffer buffer) {
        return buffer.capacity() == HaveMessage.len() && buffer.get(ID_INDEX) == HaveMessage.id();
    }

    private HaveMessage decodeHave(ByteBuffer buffer) {
        buffer.position(DATA_INDEX);
        var message = new HaveMessage();
        message.setPieceIndex(buffer.getInt());
        return message;
    }

    private boolean isChoke(ByteBuffer buffer) {
        return buffer.capacity() == ChokeMessage.len() && buffer.get(ID_INDEX) == ChokeMessage.id();
    }

    private boolean isCancel(ByteBuffer buffer) {
        return buffer.capacity() == CancelMessage.len() && buffer.get(ID_INDEX) == CancelMessage.id();
    }

    private CancelMessage decodeCancel(ByteBuffer buffer) {
        buffer.position(DATA_INDEX);
        var message = new CancelMessage();
        message.setIndex(buffer.getInt());
        message.setBegin(buffer.getInt());
        message.setLength(buffer.getInt());
        return message;
    }

    private boolean isBitfield(ByteBuffer buffer) {
        return buffer.capacity() > 5 && buffer.get(ID_INDEX) == BitfieldMessage.id();
    }

    private BitfieldMessage decodeBitfield(ByteBuffer buffer) {
        buffer.position(DATA_INDEX);
        var bitfield = new Bitfield((buffer.array().length -5)*8);
        var index = 0;
        while(buffer.hasRemaining()) {
            var item = buffer.get();
            for(var i = 7; i >= 0; i--) {
                if (((item >> i) & 0x01) == 1) {
                    bitfield.have(index);
                }
                index++;
            }
        }
        var message = new BitfieldMessage();
        message.setBitfield(bitfield);
        return message;
    }

    private boolean isHandshake(ByteBuffer buffer) {
        buffer.rewind();
        if (buffer.capacity() == HandshakeMessage.len()) {
            var pstrLen = buffer.get();
            var array = new byte[HandshakeMessage.HANDSHAKE_PSTRLEN];
            buffer.get(array);
            var pstr = new String(array);
            return pstrLen == HandshakeMessage.HANDSHAKE_PSTRLEN && pstr.equals(HandshakeMessage.HANDSHAKE_PSTR);
        }
        return false;
    }

    private HandshakeMessage decodeHandshake(ByteBuffer buffer) {
        buffer.position(28); // FIXME в константу
        var message = new HandshakeMessage();
        var infoHash = new byte[20]; // FIXME в константу
        var peerId = new byte[20];  // FIXME в контанту
        buffer.get(infoHash);
        buffer.get(peerId);
        message.setInfoHash(infoHash);
        message.setPeerId(peerId);
        return message;
    }
}
