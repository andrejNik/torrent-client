package nikonov.torrentclient.gui.service.manager;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import nikonov.torrentclient.gui.domain.event.CurrentTorrentChangeEvent;
import nikonov.torrentclient.gui.domain.event.Event;
import nikonov.torrentclient.gui.domain.event.TorrentFileNeedDownloadChangeEvent;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;
import static java.text.MessageFormat.format;

import java.util.Map;
import java.util.ResourceBundle;

public class TorrentInfoPanelManager implements EventService.EventListener {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("nikonov/torrentclient/gui/message/message");
    private static final String PEER_KEY = "torrent-info.peer-label";
    private static final String ACTIVE_PEER_KEY = "torrent-info.active-peer-label";
    private static final String DOWNLOAD_KEY = "torrent-info.save";
    private static final String DOWNLOAD_LABEL_KEY = "torrent-info.download-label";
    private static final String UPLOAD_LABEL_KEY = "torrent-info.upload-label";

    private final VBox torrentInfoPanel;
    private final Label downloadLabel;
    private final Label uploadLabel;
    private final Label activePeerLabel;
    private final Label peerLabel;
    private final Label saveButton;

    private final CurrentTorrentService currentTorrentService;

    public TorrentInfoPanelManager(VBox torrentInfoPanel,
                                   Label downloadLabel,
                                   Label uploadLabel,
                                   Label peerLabel,
                                   Label activePeerLabel,
                                   Label saveButton,
                                   CurrentTorrentService currentTorrentService) {
        this.torrentInfoPanel = torrentInfoPanel;
        this.downloadLabel = downloadLabel;
        this.uploadLabel = uploadLabel;
        this.peerLabel = peerLabel;
        this.activePeerLabel = activePeerLabel;
        this.saveButton = saveButton;
        this.currentTorrentService = currentTorrentService;
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof CurrentTorrentChangeEvent) {
            fill();
        }
        if (event instanceof TorrentFileNeedDownloadChangeEvent) {
            downloadLabel();
        }
    }

    public void fill() {
        if ( currentTorrentService.currentTorrentData() != null ) {
            var currentTorrent = currentTorrentService.currentTorrentData().getTorrent();
            setLabelVisible(true);
            peerLabel.prefWidthProperty().bind(torrentInfoPanel.widthProperty());
            peerLabel.setText( message(PEER_KEY, 0) ); // FIXME
            activePeerLabel.prefWidthProperty().bind(torrentInfoPanel.widthProperty());
            activePeerLabel.setText( message(ACTIVE_PEER_KEY, 0) ); // FIXME
            downloadLabel();
            uploadLabel();
            saveButton.prefWidthProperty().bind(torrentInfoPanel.widthProperty());
            saveButton.setText(message(DOWNLOAD_KEY));
            saveButton.setAlignment(Pos.CENTER);
        } else {
            setLabelVisible( false );
        }
    }

    private void downloadLabel() {
        var currentTorrent = currentTorrentService.currentTorrentData().getTorrent();
        downloadLabel.prefWidthProperty().bind(torrentInfoPanel.widthProperty());
        var text = message(DOWNLOAD_LABEL_KEY,
                toGByte( currentTorrent.getByteDownload() ),
                toGByte( currentTorrent.countNeedDownloadBytes() ));
        downloadLabel.setText(text.replace(',', '.'));
    }

    private void uploadLabel() {
        var currentTorrent = currentTorrentService.currentTorrentData().getTorrent();
        uploadLabel.prefWidthProperty().bind(torrentInfoPanel.widthProperty());
        var text = message(UPLOAD_LABEL_KEY,
                toGByte(currentTorrent.getByteUpload()),
                toGByte(currentTorrent.totalBytes()));
        uploadLabel.setText(text.replace(',', '.'));
    }

    private double toGByte(double countBytes) {
        return countBytes / Math.pow(10, 9);
    }

    private String message(String key, Object... args) {
        return format(
                RESOURCE_BUNDLE.getString(key), args
        );
    }

    private void setLabelVisible(boolean visible) {
        peerLabel.setVisible(visible);
        activePeerLabel.setVisible(visible);
        downloadLabel.setVisible(visible);
        uploadLabel.setVisible(visible);
        saveButton.setVisible(visible);
    }
}
