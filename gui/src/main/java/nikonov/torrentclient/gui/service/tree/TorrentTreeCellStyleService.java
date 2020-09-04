package nikonov.torrentclient.gui.service.tree;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nikonov.torrentclient.gui.domain.CurrentTorrentData;
import nikonov.torrentclient.gui.domain.FilePlayState;
import nikonov.torrentclient.gui.domain.ui.TorrentTreeCellData;
import nikonov.torrentclient.gui.service.CurrentTorrentService;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис стилизаций кнопок дерева торрент файла
 */
public class TorrentTreeCellStyleService {

    public static final String CELL_CSS_CLASS = "custom-tree-cell";
    private static final String TREE_BUTTON_CSS_CLASS = "tree-button";
    private static final String INACTIVE_PLAY_BUTTON_CSS_CLASS = "inactive-play-button";
    private static final String ACTIVE_PLAY_BUTTON_CSS_CLASS = "active-play-button";
    public static final String PAUSE_PLAY_BUTTON_CSS_CLASS = "pause-play-button";
    private static final String ACTIVE_CHECK_BUTTON_CSS_CLASS = "active-check-button";
    private static final String INACTIVE_CHECK_BUTTON_CSS_CLASS = "inactive-check-button";
    private static final String ROOT_FOLDER_BUTTON_CSS_CLASS = "root-folder-button";
    private static final String TREE_LEAF_LABEL_CSS_CLASS = "tree-leaf-label";
    private static final String TREE_FOLDER_LABEL_CSS_CLASS = "tree-folder-label";
    private static final String TREE_ROOT_FOLDER_LABEL_CSS_CLASS = "tree-root-folder-label";
    private static final String IMAGE_FOLDER_PATH = "/nikonov/torrentclient/gui/image/folder.png";

    private final Map<TorrentTreeCellData, LeafCellUIData> leafCellDataMap;
    private final CurrentTorrentService currentTorrentService;

    public TorrentTreeCellStyleService(CurrentTorrentService currentTorrentService) {
        this.currentTorrentService = currentTorrentService;
        this.leafCellDataMap = new HashMap<>();
    }

    public void styleRootCell(Button chooserRootFolder,
                              Label rootFolderLabel) {
        chooserRootFolder.getStyleClass().addAll(TREE_BUTTON_CSS_CLASS, ROOT_FOLDER_BUTTON_CSS_CLASS);
        rootFolderLabel.getStyleClass().add(TREE_ROOT_FOLDER_LABEL_CSS_CLASS);
    }

    public void styleFolderCell(ImageView folderView, Label folderName) {
        folderView.setImage(new Image(getClass().getResourceAsStream(IMAGE_FOLDER_PATH)));
        folderName.getStyleClass().add(TREE_FOLDER_LABEL_CSS_CLASS);
    }

    /**
     * стилизовать новую ячейку
     */
    public void styleNewLeafCell(TorrentTreeCellData cellData,
                                 Button playButton,
                                 Button checkButton,
                                 Label leafLabel) {
        leafCellDataMap.put(cellData, new LeafCellUIData(playButton, checkButton));
        styleNewPlayButton(cellData, playButton);
        styleNewCheckButton(cellData, checkButton);
        styleNewLeafLabel(leafLabel);
    }

    /**
     * стилизовать активную кнопку проигрывания
     */
    public void stylePlayButton(Button playButton) {
        leafCellDataMap.forEach((key, value) -> {
            value.getPlayButton().getStyleClass().clear();
            value.getPlayButton().getStyleClass().addAll(TREE_BUTTON_CSS_CLASS, INACTIVE_PLAY_BUTTON_CSS_CLASS);
        });
        playButton.getStyleClass().remove(INACTIVE_PLAY_BUTTON_CSS_CLASS);
        playButton.getStyleClass().add(currentTorrentService.currentTorrentData().getState() == FilePlayState.PLAY ?
                ACTIVE_PLAY_BUTTON_CSS_CLASS : PAUSE_PLAY_BUTTON_CSS_CLASS);
    }

    /**
     * стилизовать кнопку пометки загрузки
     */
    public void styleCheckButton(Button checkButton) {
        var active = checkButton.getStyleClass().contains(ACTIVE_CHECK_BUTTON_CSS_CLASS);
        checkButton.getStyleClass().clear();
        checkButton.getStyleClass().add(TREE_BUTTON_CSS_CLASS);
        if (active) {
            checkButton.getStyleClass().add(INACTIVE_CHECK_BUTTON_CSS_CLASS);
        } else {
            checkButton.getStyleClass().add(ACTIVE_CHECK_BUTTON_CSS_CLASS);
        }
    }

    /**
     * стилизовать новую кнопку проигрывания
     */
    private void styleNewPlayButton(TorrentTreeCellData cellData, Button playButton) {
        playButton.getStyleClass().add(TREE_BUTTON_CSS_CLASS);
        var currentTorrentData = currentTorrentService.currentTorrentData();
        var cellTorrentData = new CurrentTorrentData(cellData.getTorrent(), cellData.getFile(), FilePlayState.NONE);
        if ( currentTorrentData != null && currentTorrentData.equalsWithoutState(cellTorrentData) ) {
            playButton.getStyleClass().add(currentTorrentData.getState() == FilePlayState.PLAY ?
                    ACTIVE_PLAY_BUTTON_CSS_CLASS : PAUSE_PLAY_BUTTON_CSS_CLASS);
        } else {
            playButton.getStyleClass().add(INACTIVE_PLAY_BUTTON_CSS_CLASS);
        }
    }

    /**
     * стилизовать кнопку пометки загрузки
     */
    private void styleNewCheckButton(TorrentTreeCellData cellData, Button checkButton) {
        checkButton.getStyleClass().add(TREE_BUTTON_CSS_CLASS);
        if (cellData.getFile().isNeedDownload()) {
            checkButton.getStyleClass().add(ACTIVE_CHECK_BUTTON_CSS_CLASS);
        } else {
            checkButton.getStyleClass().add(INACTIVE_CHECK_BUTTON_CSS_CLASS);
        }
    }

    private void styleNewLeafLabel(Label leafLabel) {
        leafLabel.getStyleClass().add(TREE_LEAF_LABEL_CSS_CLASS);
    }

    @Getter
    @AllArgsConstructor
    private static class LeafCellUIData {
        private final Button playButton;
        private final Button checkButton;
    }
}
