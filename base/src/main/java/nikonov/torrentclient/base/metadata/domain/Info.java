package nikonov.torrentclient.base.metadata.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Представление словаря info
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Info {
    /**
     * Размер куска в байтах
     */
    private int pieceLength;
    /**
     * SHA1 хеши кусков
     */
    private byte[][] pieceHashes;
    /**
     * Должен ли клиент получать список пиров только от трекеров
     */
    private boolean onlyTrackers;
    /**
     * Файлы торрента
     */
    private List<File> files;
    /**
     * 20-байтовый SHA1-хеш значения ключа info файла метаданных
     */
    private byte[] sha1Hash;
}
