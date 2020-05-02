package nikonov.torrentclient.download;

import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.network.domain.message.*;

/**
 * сервис загрузки торрента
 */
public interface DownloadService {
    /**
     * установлено соединение с новым пиром
     */
    void connect(PeerAddress peerAddress);

    /**
     * потеряно/закрыто соединение с пиром
     */
    void disconnect(PeerAddress peerAddress);

    /**
     * пир прислал handshake сообщение
     */
    void handshake(HandshakeMessage handshakeMessage);

    /**
     * пир прислал bitfield сообщение
     */
    void bitfieldMessage(BitfieldMessage bitfieldMessage);

    /**
     * пир прислал choke сообщение
     */
    void chokeMessage(ChokeMessage chokeMessage);

    /**
     * пир прислал unchoke сообщение
     */
    void unchokeMessage(UnchokeMessage unchokeMessage);

    /**
     * пир прислал have сообщение
     */
    void haveMessage(HaveMessage haveMessage);

    /**
     * пир прислал piece сообщение
     *
     * @return скачан ли весь кусок
     */
    boolean pieceMessage(PieceMessage pieceMessage);

    /**
     * начать загрузку
     */
    void download();
}
