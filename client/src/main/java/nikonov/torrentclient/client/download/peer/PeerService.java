package nikonov.torrentclient.client.download.peer;

import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.download.domain.peer.Peer;
import nikonov.torrentclient.client.network.domain.message.*;

import java.util.List;

/**
 * сервис связи с пирами
 */
public interface PeerService {

    /**
     * установлено соединение с пиром
     */
    void connect(PeerAddress peerAddress);

    /**
     * соединение с пиром закрыто
     */
    void disconnect(PeerAddress peerAddress);

    /**
     * пир прислал handshake сообщение
     */
    void handshakeMessage(HandshakeMessage handshakeMessage);

    /**
     * пир прислал bitfield сообщение
     */
    void bitfieldMessage(BitfieldMessage bitfieldMessage);

    /**
     * пир прислал unchoke сообщение
     */
    void unchokeMessage(UnchokeMessage unchokeMessage);

    /**
     * пир прислал choke сообщение
     */
    void chokeMessage(ChokeMessage chokeMessage);

    /**
     * пир прислал have сообщение
     */
    void haveMessage(HaveMessage haveMessage);
    /**
     * оповестить сервис о том что кусок скачан
     */
    void pieceDownload(int pieceIndex);

    /**
     * список пиров
     */
    List<Peer> peers();
}
