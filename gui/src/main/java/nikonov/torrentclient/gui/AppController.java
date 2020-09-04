package nikonov.torrentclient.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Window;
import nikonov.torrentclient.gui.domain.ui.TorrentTreeCellData;
import nikonov.torrentclient.gui.domain.Torrent;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;
import nikonov.torrentclient.gui.service.TorrentService;
import nikonov.torrentclient.gui.service.manager.*;
import nikonov.torrentclient.gui.service.tree.TorrentTreeCellActionHandler;

import java.util.List;

public class AppController {
    @FXML
    private ImageView videoImageView;
    @FXML
    private VBox videoPanel;
    /**
     * Левая панель со списком торрентов
     */
    @FXML
    private VBox torrentListPanel;
    /**
     * Центральная панель с данными о отображаем торренте
     */
    @FXML
    private VBox displayTorrentPanel;
    /**
     * Кнопка добавления нового торрента
     */
    @FXML
    private Label addTorrentLabel;
    /**
     * Панель-заголовок отображаемого торрента
     */
    @FXML
    private StackPane displayTorrentHeadPanel;
    /**
     * Прогресс загрузки отображаемого торрента
     */
    @FXML
    private ProgressBar displayTorrentProgressBar;
    /**
     * Label с названием отображаемого торрента
     */
    @FXML
    private Label displayTorrentNameLabel;
    /**
     * Дерево файлов отображаемого торрента
     */
    @FXML
    private TreeView<TorrentTreeCellData> displayTorrentFileTree;

    /**
     * Нижняя панель с общей информацией по торренту
     */
    @FXML
    private VBox torrentInfoPanel;
    @FXML
    private Label downloadLabel;
    @FXML
    private Label uploadLabel;
    @FXML
    private Label peerLabel;
    @FXML
    private Label activePeerLabel;
    @FXML
    private Label saveButton;

    private TorrentTreeManager torrentTreeManager;
    private TorrentListPanelManager torrentListPanelManager;
    private TorrentHeadPanelManager torrentHeadPanelManager;
    private TorrentInfoPanelManager torrentInfoPanelManager;
    private TorrentPlayerManager torrentPlayerManager;
    private TorrentService torrentService;
    private CurrentTorrentService currentTorrentService;
    private EventService eventService;
    private Window window;

    public AppController(Window window) {
        this.window = window;
    }

    @FXML
    public void initialize() {
        this.torrentService = new TorrentService();
        this.eventService = new EventService();
        this.currentTorrentService = new CurrentTorrentService();
        this.torrentTreeManager = new TorrentTreeManager(
                displayTorrentFileTree,
                new TorrentTreeCellActionHandler(currentTorrentService, eventService),
                currentTorrentService,
                window);
        this.torrentListPanelManager = new TorrentListPanelManager(
                torrentService,
                currentTorrentService,
                eventService,
                torrentListPanel,
                addTorrentLabel,
                window);
        this.torrentHeadPanelManager = new TorrentHeadPanelManager(
                currentTorrentService,
                displayTorrentPanel,
                displayTorrentHeadPanel,
                displayTorrentNameLabel,
                displayTorrentProgressBar);
        this.torrentPlayerManager = new TorrentPlayerManager(currentTorrentService, videoPanel, videoImageView);
        this.torrentInfoPanelManager = new TorrentInfoPanelManager(
                torrentInfoPanel,
                downloadLabel,
                uploadLabel,
                peerLabel,
                activePeerLabel,
                saveButton,
                currentTorrentService
        );
        registerEventListeners();
        fillPanels(torrentService.torrents());
    }

    private void registerEventListeners() {
        eventService.addEventListener(torrentListPanelManager);
        eventService.addEventListener(torrentTreeManager);
        eventService.addEventListener(torrentHeadPanelManager);
        eventService.addEventListener(torrentPlayerManager);
        eventService.addEventListener(torrentInfoPanelManager);
    }

    public void destroy() {
        torrentPlayerManager.destroy();
    }

    /**
     * Заполнить панели окна - список и панель с данными текущего торрента
     */
    private void fillPanels(List<Torrent> torrents) {
        torrentListPanelManager.fill();
        torrentTreeManager.fill();
        torrentHeadPanelManager.fill();
        torrentInfoPanelManager.fill();
        torrentPlayerManager.fill();
    }
}
