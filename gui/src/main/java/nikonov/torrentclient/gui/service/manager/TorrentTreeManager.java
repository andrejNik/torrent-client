package nikonov.torrentclient.gui.service.manager;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Window;
import nikonov.torrentclient.gui.domain.event.CurrentTorrentChangeEvent;
import nikonov.torrentclient.gui.domain.event.Event;
import nikonov.torrentclient.gui.domain.ui.TorrentTreeCellData;
import nikonov.torrentclient.gui.domain.ui.TorrentTreeCell;
import nikonov.torrentclient.gui.service.CurrentTorrentService;
import nikonov.torrentclient.gui.service.EventService;
import nikonov.torrentclient.gui.service.tree.TorrentTreeCellActionHandler;
import nikonov.torrentclient.gui.service.tree.TorrentTreeCellStyleService;

import java.util.HashMap;

/**
 * Менеджер управления деревом торрента
 */
public class TorrentTreeManager implements EventService.EventListener {

    private final TreeView<TorrentTreeCellData> tree;
    private final TorrentTreeCellStyleService treeCellStyleService;
    private final TorrentTreeCellActionHandler actionHandler;
    private final CurrentTorrentService currentTorrentService;

    public final Window window;

    public TorrentTreeManager(TreeView<TorrentTreeCellData> tree,
                              TorrentTreeCellActionHandler actionHandler,
                              CurrentTorrentService currentTorrentService,
                              Window window) {
        this.tree = tree;
        this.actionHandler = actionHandler;
        this.treeCellStyleService = new TorrentTreeCellStyleService(currentTorrentService);
        this.currentTorrentService = currentTorrentService;
        this.window = window;
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof CurrentTorrentChangeEvent) {
            fill();
        }
    }

    /**
     * Заполнить дерево данными о торренте
     */
    public void fill() {
        if (currentTorrentService.currentTorrentData() != null) {
            var root = root();
            tree.setRoot(root);
            tree.setEditable(true);
            tree.setCellFactory(tree -> new TorrentTreeCell(currentTorrentService, treeCellStyleService, actionHandler, window));
        }
    }

    private TreeItem<TorrentTreeCellData> root() {
        var torrent = currentTorrentService.currentTorrentData().getTorrent();
        var rootData = new TorrentTreeCellData(torrent, null, torrent.getRootDirectory());
        rootData.setRoot(true);
        var root = new TreeItem<>(rootData);
        root.setExpanded(true);
        root.addEventHandler(TreeItem.branchCollapsedEvent(), event -> event.getTreeItem().setExpanded(true));
        var treeItemMap = new HashMap<String, TreeItem<TorrentTreeCellData>>();
        for(var file : torrent.getFiles()) {
            var itemArray = file.getPath().split("\\\\"); // FIXME РАЗДЕЛИТЕЛЬ В ЗАВИСИМОСТИ ОТ ОС ( смотреть TorrentService )
            for(var i = 0; i < itemArray.length; i++) {
                var itemFull = fullItemName(itemArray, i);
                var itemShort = itemArray[i];
                if (!treeItemMap.containsKey(itemFull)) {
                    var treeItem = new TreeItem<>(new TorrentTreeCellData(torrent, file, itemShort));
                    treeItem.setExpanded(true);
                    treeItemMap.put(itemFull, treeItem);
                    if (i == 0) {
                        root.getChildren().add(treeItem);
                    } else { // проверка что последний -> добавить checkbox
                        var parentItem = fullItemName(itemArray, i - 1);
                        var parentTreeItem = treeItemMap.get(parentItem);
                        parentTreeItem.getChildren().add(treeItem);
                    }
                }
            }
        }
        return root;
    }

    private String fullItemName(String[] itemArray, int itemIndex) {
        StringBuilder item = new StringBuilder();
        for(var i = 0; i <= itemIndex; i++) {
            if (i != 0) {
                item.append("\\").append(itemArray[i]); // FIXME РАЗДЕЛИТЕЛЬ В ЗАВИСИМОСТИ ОТ ОСИ ( смотреть TorrentService )
            } else {
                item.append(itemArray[i]);
            }
        }
        return item.toString();
    }
}
