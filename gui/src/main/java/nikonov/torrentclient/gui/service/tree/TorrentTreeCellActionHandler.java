package nikonov.torrentclient.gui.service.tree;

import nikonov.torrentclient.gui.domain.*;
import nikonov.torrentclient.gui.domain.event.TorrentFileNeedDownloadChangeEvent;
import nikonov.torrentclient.gui.domain.event.TorrentFilePauseEvent;
import nikonov.torrentclient.gui.domain.event.TorrentFilePlayEvent;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;

/**
 * Обработчик событий дерева торрента
 */
public class TorrentTreeCellActionHandler {

    private final CurrentTorrentService currentTorrentService;
    private final EventService eventService;

    public TorrentTreeCellActionHandler(CurrentTorrentService currentTorrentService,
                                        EventService eventService) {
        this.currentTorrentService = currentTorrentService;
        this.eventService = eventService;
    }

    public void playAction(Torrent torrent, TorrentFile torrentFile) {
        var currentTorrentData = currentTorrentService.currentTorrentData();
        var actionTorrentData = new CurrentTorrentData(torrent, torrentFile, null);
        if (currentTorrentData != null && currentTorrentData.equalsWithoutState(actionTorrentData)) {
            currentTorrentService.setCurrentTorrentData(new CurrentTorrentData(
                    torrent,
                    torrentFile,
                    currentTorrentData.getState() == FilePlayState.PLAY ? FilePlayState.PAUSE : FilePlayState.PLAY));
        } else {
            currentTorrentService.setCurrentTorrentData(new CurrentTorrentData(torrent, torrentFile, FilePlayState.PLAY));
        }
        eventService.publishEvent(currentTorrentService.currentTorrentData().getState() == FilePlayState.PLAY ?
                new TorrentFilePlayEvent() : new TorrentFilePauseEvent());
    }

    public void checkButtonAction(Torrent torrent, TorrentFile torrentFile) {
        torrentFile.setNeedDownload(!torrentFile.isNeedDownload());
        eventService.publishEvent(new TorrentFileNeedDownloadChangeEvent());
    }
}
