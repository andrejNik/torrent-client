package nikonov.torrentclient.metadata.domain;

/**
 * Ключи торрент файла
 */
public class TorrentFileKey {
    /**
     * Аnnounce-URL трекера
     */
    public static final String ANNOUNCE = "announce";
    /**
     * Список резервных трекеров
     */
    public static final String ANNOUNCE_LIST = "announce-list";
    /**
     * Дата создания в UNIX времени
     */
    public static final String CREATION_DATE = "creation date";
    /**
     * Комментарий от автора
     */
    public static final String COMMENT = "comment";
    /**
     * Имя и версия программы, которая использовалась для создания торрент файла
     */
    public static final String CREATED_BY = "created by";
    /**
     * Словарь, описывающий файлы торрента
     */
    public static final String INFO = "info";
    /**
     * Размер каждого куска в байтах
     */
    public static final String INFO_PIECE_LENGTH = "piece length";
    /**
     * Строка, составленная объединением всех 20-байтовых значений SHA1-хешей
     */
    public static final String INFO_PIECES = "pieces";
    /**
     *
     */
    public static final String INFO_PRIVATE = "private";
    /**
     * Зависит от торрента
     * 1. Имя файла
     * 2. Имя директорий в которой содержатся все файлы
     */
    public static final String INFO_NAME = "name";
    /**
     * Размер файла в байтах
     */
    public static final String INFO_LENGTH = "length";
    /**
     * Список словарей на каждый файл.
     */
    public static final String INFO_FILES = "files";
    /**
     * Список, содержащий один или более элементов, объединение которых дает дает путь и имя файла
     */
    public static final String INFO_PATH = "path";
}
