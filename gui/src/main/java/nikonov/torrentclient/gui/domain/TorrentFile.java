package nikonov.torrentclient.gui.domain;

import lombok.*;

/**
 * Файл торрента
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TorrentFile {
    /**
     * Индекс файла, указанный в торрент-файле
     */
    private int index;
    /**
     * Путь сохранения файла торрента относительно {@link Torrent#getRootDirectory()}
     */
    private String path;
    /**
     * Имя файла
     */
    private String name;
    /**
     * Нужно ли его загружать
     */
    private boolean needDownload = true;
    /**
     * Длина файла в байтах
     */
    private long length;
}
