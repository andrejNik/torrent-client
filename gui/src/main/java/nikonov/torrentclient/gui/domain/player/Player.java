package nikonov.torrentclient.gui.domain.player;

import nikonov.torrentclient.gui.domain.TorrentFile;

/**
 * Интерфейс плеера
 */
public interface Player {

    /**
     * воспроизвести указанный торрент файл;
     */
    void play(TorrentFile file);

    /**
     * освободить ресурсы плеера
     */
    default void destroy() {

    }
}
