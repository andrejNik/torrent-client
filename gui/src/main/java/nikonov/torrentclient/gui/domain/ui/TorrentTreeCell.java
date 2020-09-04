package nikonov.torrentclient.gui.domain.ui;

import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import nikonov.torrentclient.gui.domain.FilePlayState;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.tree.TorrentTreeCellActionHandler;
import nikonov.torrentclient.gui.service.tree.TorrentTreeCellStyleService;
import static java.text.MessageFormat.format;

import java.io.File;
import java.util.ResourceBundle;

/**
 * TODO убрать actionHandler и генерировать события
 */
public class TorrentTreeCell extends TreeCell<TorrentTreeCellData> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("nikonov/torrentclient/gui/message/message");
    private static final PseudoClass ROOT = PseudoClass.getPseudoClass("root");

    private static final String PLAY_BUTTON_TOOLTIP_KEY = "torrent-tree.play-button.tooltip";
    private static final String PLAY_STOP_BUTTON_TOOLTIP_KEY = "torrent-tree.play-button.tooltip.stop";
    private static final String ROOT_FOLDER_TOOLTIP_KEY = "torrent-tree.root-folder.tooltip";
    private static final String ROOT_FOLDER_PREFIX_KEY = "torrent-tree.root-folder-prefix";
    private static final String CHECK_BUTTON_ADD_TOOLTIP_KEY = "torrent-tree.check-button.tooltip.add";
    private static final String CHECK_BUTTON_REMOVE_TOOLTIP_KEY = "torrent-tree.check-button.tooltip.remove";
    private static final int BOX_SPACING = 3;

    private final CurrentTorrentService currentTorrentService;
    private final TorrentTreeCellStyleService cellStyleService;
    private final TorrentTreeCellActionHandler actionHandler;
    private final Window window;

    public TorrentTreeCell(CurrentTorrentService currentTorrentService,
                           TorrentTreeCellStyleService cellStyleService,
                           TorrentTreeCellActionHandler actionHandler,
                           Window window) {
        this.currentTorrentService = currentTorrentService;
        this.cellStyleService = cellStyleService;
        this.actionHandler = actionHandler;
        this.window = window;

        getStyleClass().add(TorrentTreeCellStyleService.CELL_CSS_CLASS);
        treeViewProperty().addListener(observable -> pseudoClassStateChanged(ROOT, true));
        treeItemProperty().addListener(observable -> pseudoClassStateChanged(ROOT, true));
    }

    @Override
    protected void updateItem(TorrentTreeCellData cellData, boolean empty) {
        super.updateItem(cellData, empty);
        if (isEmpty()) {
            setGraphic(null);
        } else {
            var box = boxCell(getTreeItem().isLeaf(), cellData.isRoot(), cellData);
            setGraphic(box);
        }
        setText(null);
    }

    private HBox boxCell(boolean isLeaf, boolean isRoot, TorrentTreeCellData cellData) {
        if ( isLeaf )
            return createLeafCell(cellData);
        return isRoot ? createRootCell(cellData) : createFolderCell(cellData);
    }

    private HBox createLeafCell(TorrentTreeCellData cellData) {
        var box = new HBox(BOX_SPACING);
        var check = createCheckButton(cellData);
        var label = new Label(cellData.getDisplayName());
        var playButton = createPlayButton(cellData);
        box.getChildren().addAll(check, playButton, label);
        cellStyleService.styleNewLeafCell(cellData, playButton, check, label);
        return box;
    }

    private HBox createRootCell(TorrentTreeCellData cellData) {
        var box = new HBox(BOX_SPACING);
        var label = new Label(format(
                RESOURCE_BUNDLE.getString(ROOT_FOLDER_PREFIX_KEY),
                cellData.getTorrent().getRootDirectory()
        ));
        var chooser = new DirectoryChooser();
        var chooserButton = new Button();
        chooserButton.setTooltip(new Tooltip(RESOURCE_BUNDLE.getString(ROOT_FOLDER_TOOLTIP_KEY)));
        chooserButton.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            var selectedDirectory = chooser.showDialog(window);
            if (selectedDirectory != null) {
                cellData.getTorrent().setRootDirectory(selectedDirectory.getAbsolutePath());
                label.setText(format(
                        RESOURCE_BUNDLE.getString(ROOT_FOLDER_PREFIX_KEY),
                        selectedDirectory.getAbsolutePath()));
            }
        });
        chooser.setInitialDirectory(new File(cellData.getTorrent().getRootDirectory()));
        cellStyleService.styleRootCell(chooserButton, label);
        box.getChildren().addAll(chooserButton, label);
        return box;
    }

    private HBox createFolderCell(TorrentTreeCellData cellData) {
        var box = new HBox(BOX_SPACING);
        var label = new Label(cellData.getDisplayName());
        var imageView = new ImageView();
        cellStyleService.styleFolderCell(imageView, label);
        box.getChildren().addAll(imageView, label);
        return box;
    }

    private Button createCheckButton(TorrentTreeCellData cellData) {
        var checkButton = new Button();
        checkButton.setTooltip(new Tooltip(format(
                RESOURCE_BUNDLE.getString(cellData.getFile().isNeedDownload() ? CHECK_BUTTON_REMOVE_TOOLTIP_KEY : CHECK_BUTTON_ADD_TOOLTIP_KEY),
                cellData.getDisplayName()))
        );
        checkButton.setOnAction(event -> {
            actionHandler.checkButtonAction(cellData.getTorrent(), cellData.getFile());
            cellStyleService.styleCheckButton(checkButton);
            checkButton.setTooltip(new Tooltip(format(
                    RESOURCE_BUNDLE.getString(cellData.getFile().isNeedDownload() ? CHECK_BUTTON_REMOVE_TOOLTIP_KEY : CHECK_BUTTON_ADD_TOOLTIP_KEY),
                    cellData.getDisplayName()))
            );
        });
        return checkButton;
    }

    private Button createPlayButton(TorrentTreeCellData cellData) {
        var button = new Button();
        button.setTooltip(new Tooltip(format(
                RESOURCE_BUNDLE.getString(PLAY_BUTTON_TOOLTIP_KEY),
                cellData.getDisplayName())));
        button.setOnAction(event -> {
            actionHandler.playAction(cellData.getTorrent(), cellData.getFile());
            button.setTooltip(new Tooltip(format(
                    RESOURCE_BUNDLE.getString(currentTorrentService.currentTorrentData().getState() == FilePlayState.PLAY ? PLAY_STOP_BUTTON_TOOLTIP_KEY : PLAY_BUTTON_TOOLTIP_KEY),
                    cellData.getDisplayName())));
            cellStyleService.stylePlayButton(button);
        });
        return button;
    }
}
