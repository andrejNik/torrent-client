package nikonov.torrentclient.gui.service;

import nikonov.torrentclient.gui.domain.CurrentTorrentData;

/**
 * Сервис упавления текущим видимым торрентом
 */
public class CurrentTorrentService {

    private CurrentTorrentData currentTorrentData;

    public CurrentTorrentData currentTorrentData() {
        return currentTorrentData;
    }

    public void setCurrentTorrentData(CurrentTorrentData currentTorrentData) {
        this.currentTorrentData = currentTorrentData;
    }
}
