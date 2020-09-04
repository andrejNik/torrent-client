package nikonov.torrentclient.gui.domain;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Информация по торренту
 * TODO ПЕРЕВЕСТИ ВСЕ НА METADATA ?
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Torrent {
    /**
     * Идентификатор торрента
     */
    private int id;
    /**
     * Корневая директория торрента
     */
    private String rootDirectory = System.getProperty("user.home");
    /**
     * Имя торрента
     */
    private String name;
    /**
     * Состояние торрента
     */
    private TorrentState state = TorrentState.INACTIVE;
    /**
     * Байт загружено
     */
    private long byteDownload;
    /**
     * Байт отдано
     */
    private long byteUpload;
    /**
     * Список файлов торрента
     */
    private List<TorrentFile> files = new ArrayList<>();
    /**
     * Дата создания
     */
    private Instant creationDate;
    /**
     * Создано
     */
    private String createdBy;
    /**
     * Комментарий
     */
    private String comment;

    /**
     * сколько байтов нужно загрузить
     */
    public long countNeedDownloadBytes() {
        return files.stream().filter(TorrentFile::isNeedDownload).mapToLong(TorrentFile::getLength).sum();
    }

    public long totalBytes() {
        return files.stream().mapToLong(TorrentFile::getLength).sum();
    }
}
