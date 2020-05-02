package nikonov.torrentclient.filestorage;

import nikonov.torrentclient.download.DownloadServiceImpl;

/**
 * Интерфейс потребителя кусков, полученых от сервиса загрузки {@link DownloadServiceImpl}
 * todo: Возможно стоит перенести этот интерфейс из этого пакета, тк пакет предназначен для файлового потребителя, а не потребителя в целом
 * todo: Но тк других реализаций пока не предвидится, пусть будет тут
 */
public interface PieceConsumerService {

    void apply(int pieceIndex, byte[] piece);
}
