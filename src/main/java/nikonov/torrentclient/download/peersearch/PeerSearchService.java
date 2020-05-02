package nikonov.torrentclient.download.peersearch;

import nikonov.torrentclient.domain.PeerAddress;

/**
 * Сервис поиска пиров
 */
public interface PeerSearchService {
    /**
     * начать поиск
     */
    void start();

    /**
     * завершить поиск
     */
    void stop();

    /**
     * появление активного пира
     * (пир прислал unchoke сообщение)
     */
    void activePeer(PeerAddress peerAddress);

    /**
     * пир стал неактивный
     * (пир прислал choke сообщение или закрыл соединение)
     */
    void inactivePeer(PeerAddress peerAddress);

    /**
     * скачан блок указанной длины
     */
    void pieceDownload(int pieceLength);

    /**
     * отдан блок указанной длины
     */
    void pieceUpload(int pieceLength);

    /**
     * загрузка торрента завершена
     */
    void complete();
}
