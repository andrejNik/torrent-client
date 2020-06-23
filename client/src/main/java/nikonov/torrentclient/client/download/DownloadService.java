package nikonov.torrentclient.client.download;

import nikonov.torrentclient.client.network.domain.message.*;

/**
 * сервис загрузки торрента
 */
public interface DownloadService {
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
