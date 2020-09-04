package nikonov.torrentclient.gui.service.manager;

import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import nikonov.torrentclient.gui.domain.event.CurrentTorrentChangeEvent;
import nikonov.torrentclient.gui.domain.event.Event;
import nikonov.torrentclient.gui.domain.event.TorrentFilePauseEvent;
import nikonov.torrentclient.gui.domain.event.TorrentFilePlayEvent;
import nikonov.torrentclient.gui.domain.player.Player;
import nikonov.torrentclient.gui.domain.player.VLCJPlayer;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;

/**
 * Менеджер управления плеером
 */
public class TorrentPlayerManager implements EventService.EventListener {

    private final Player player;
    private final VBox playerPanel;
    private final ImageView playerImageView;
    private final CurrentTorrentService currentTorrentService;

    public TorrentPlayerManager(CurrentTorrentService currentTorrentService, VBox playerPanel, ImageView playerImageView) {
        this.currentTorrentService = currentTorrentService;
        this.player = new VLCJPlayer(playerImageView);
        this.playerPanel = playerPanel;
        this.playerImageView = playerImageView;
        init();
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof CurrentTorrentChangeEvent) {
            // останавливаем воспроизведение если нужно
            fill();
        } if (event instanceof TorrentFilePlayEvent) {

        } if (event instanceof TorrentFilePauseEvent) {

        }
    }

    public void fill() {
        playerPanel.setVisible( currentTorrentService.currentTorrentData() != null );
    }

    public void destroy() {
        player.destroy();
    }

    private void init() {
        playerImageView.setPreserveRatio(true);
        playerImageView.fitWidthProperty().bind(playerPanel.widthProperty());
        playerImageView.fitHeightProperty().bind(playerPanel.heightProperty());
    }
}
