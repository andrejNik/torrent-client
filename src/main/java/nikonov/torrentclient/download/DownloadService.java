package nikonov.torrentclient.download;

import nikonov.torrentclient.network.domain.message.*;

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
