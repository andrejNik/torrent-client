package nikonov.torrentclient.gui.service.manager;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import nikonov.torrentclient.gui.domain.TorrentFile;
import nikonov.torrentclient.gui.domain.TorrentState;
import nikonov.torrentclient.gui.domain.event.CurrentTorrentChangeEvent;
import nikonov.torrentclient.gui.domain.event.Event;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;

/**
 * Менеджер управения панелью заголовка торрента ( верхняя панель c названием и прогресс баром)
 */
public class TorrentHeadPanelManager implements EventService.EventListener {

    private Label torrentNameLabel;
    private ProgressBar downloadTorrentProgressBar;
    private CurrentTorrentService currentTorrentService;

    public TorrentHeadPanelManager(CurrentTorrentService currentTorrentService,
                                   VBox mainCenterPanel,
                                   StackPane headPanel,
                                   Label torrentNameLabel,
                                   ProgressBar downloadTorrentProgressBar) {
        this.currentTorrentService = currentTorrentService;
        this.torrentNameLabel = torrentNameLabel;
        this.downloadTorrentProgressBar = downloadTorrentProgressBar;
        init(mainCenterPanel, headPanel);
    }


    @Override
    public void handleEvent(Event event) {
       if (event instanceof CurrentTorrentChangeEvent) {
           fill();
       }
    }

    public void fill() {
        if (currentTorrentService.currentTorrentData() != null) {
            setVisibleForPanelItems(true);
            var torrent = currentTorrentService.currentTorrentData().getTorrent();
            torrentNameLabel.setText(torrent.getName());
            if (torrent.getState() == TorrentState.ACTIVE) {
                var summaryLength = (double) torrent.getFiles().stream().filter(TorrentFile::isNeedDownload).mapToLong(TorrentFile::getLength).sum();
                downloadTorrentProgressBar.setProgress(torrent.getByteDownload() / summaryLength);
            }
        } else {
            setVisibleForPanelItems(false);
        }
    }

    private void init(VBox mainCenterPanel, StackPane headPanel) {
        headPanel.prefWidthProperty().bind(mainCenterPanel.widthProperty());
        downloadTorrentProgressBar.prefWidthProperty().bind(headPanel.widthProperty());
        downloadTorrentProgressBar.prefHeightProperty().bind(headPanel.heightProperty());
    }

    private void setVisibleForPanelItems(boolean visible) {
        torrentNameLabel.setVisible( visible );
        downloadTorrentProgressBar.setVisible( visible );
    }
}
