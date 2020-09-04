package nikonov.torrentclient.gui.domain;

/**
 * Состояние торрента
 */
public enum TorrentState {
    /**
     * Торрент создан, но не загружается-раздается
     */
    INACTIVE,
    /**
     * Торрент скачивается-раздается
     */
    ACTIVE,
    /**
     *
     */
    PAUSE
}
