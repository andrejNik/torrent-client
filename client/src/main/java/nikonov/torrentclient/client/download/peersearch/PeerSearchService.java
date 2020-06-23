package nikonov.torrentclient.client.download.peersearch;

import nikonov.torrentclient.client.domain.PeerAddress;

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
     * загрузка торрента завершена
     */
    void complete();

    /**
     * Соединенение с пиром закрыто.
     */
    void disconnect(PeerAddress peerAddress);
}
