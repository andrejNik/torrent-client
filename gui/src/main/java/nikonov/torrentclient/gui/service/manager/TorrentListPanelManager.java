package nikonov.torrentclient.gui.service.manager;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import nikonov.torrentclient.gui.domain.CurrentTorrentData;
import nikonov.torrentclient.gui.domain.Torrent;
import nikonov.torrentclient.gui.domain.event.CurrentTorrentChangeEvent;
import nikonov.torrentclient.gui.domain.event.Event;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;
import nikonov.torrentclient.gui.service.TorrentService;

import java.util.ResourceBundle;

/**
 * Сервис управления панелью со списком торрентов ( левая панель )
 */
public class TorrentListPanelManager implements EventService.EventListener {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("nikonov/torrentclient/gui/message/message");
    private static final String TORRENT_ADD_LABEL_KEY = "torrent-add-label";
    private static final String DISPLAY_TORRENT_LABEL_CSS_CLASS = "display-torrent-label";
    private static final String TORRENTS_LABEL_CSS_CLASS = "torrent-label";

    private final VBox torrentListPanel;
    private final Label addTorrentLabel;
    private final TorrentService torrentService;
    private final CurrentTorrentService currentTorrentService;
    private final EventService eventService;
    private final Window window;

    public TorrentListPanelManager(TorrentService torrentService,
                                   CurrentTorrentService currentTorrentService,
                                   EventService eventService,
                                   VBox torrentListPanel,
                                   Label addTorrentLabel,
                                   Window window) {
        this.torrentService = torrentService;
        this.currentTorrentService = currentTorrentService;
        this.eventService = eventService;
        this.torrentListPanel = torrentListPanel;
        this.addTorrentLabel = addTorrentLabel;
        this.window = window;
        init();
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof CurrentTorrentChangeEvent) {
            fill();
        }
    }

    public void fill() {
        deleteAllTorrentLabels();
        if ( currentTorrentService.currentTorrentData() != null ) {
            var torrentList = torrentService.torrents();
            var currentTorrent = currentTorrentService.currentTorrentData().getTorrent();
            for (var torrent : torrentList) {
                var label = new Label(torrent.getName());
                label.setTooltip(new Tooltip(torrent.getName()));
                label.getStyleClass().add(TORRENTS_LABEL_CSS_CLASS);
                if (torrent.equals(currentTorrent)) {
                    label.getStyleClass().add(DISPLAY_TORRENT_LABEL_CSS_CLASS);
                }
                label.prefWidthProperty().bind(torrentListPanel.widthProperty());
                label.setOnMouseClicked(mouseEvent -> torrentLabelClickHandler(torrent));
                torrentListPanel.getChildren().add(torrentListPanel.getChildren().size() - 1, label);
            }
        }
    }

    private void torrentLabelClickHandler(Torrent torrent) {
        currentTorrentService.setCurrentTorrentData(new CurrentTorrentData(torrent));
        eventService.publishEvent(new CurrentTorrentChangeEvent());
    }

    private void deleteAllTorrentLabels() {
        while(torrentListPanel.getChildren().size() != 1) {
            torrentListPanel.getChildren().remove(0);
        }
    }

    private void init() {
        addTorrentLabel.setText(RESOURCE_BUNDLE.getString(TORRENT_ADD_LABEL_KEY));
        addTorrentLabel.prefWidthProperty().bind(torrentListPanel.widthProperty());
        addTorrentLabel.setAlignment(Pos.CENTER);
        var fileChooser = new FileChooser();
        addTorrentLabel.setOnMouseClicked(event -> {
            var file = fileChooser.showOpenDialog(window);
            if (file != null) {
                var torrent = torrentService.create(file.getAbsolutePath());
                if (torrent != null) {
                    currentTorrentService.setCurrentTorrentData(new CurrentTorrentData(torrent));
                    eventService.publishEvent(new CurrentTorrentChangeEvent());
                }
            }
        });
    }
}
