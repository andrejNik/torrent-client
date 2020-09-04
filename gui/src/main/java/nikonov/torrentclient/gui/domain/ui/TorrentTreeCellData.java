package nikonov.torrentclient.gui.domain.ui;


import lombok.*;
import nikonov.torrentclient.gui.domain.Torrent;
import nikonov.torrentclient.gui.domain.TorrentFile;

/**
 * Данные ячейки дерева торрента
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TorrentTreeCellData {

    private String displayName;
    private Torrent torrent;
    private TorrentFile file;
    private boolean isRoot;

    public TorrentTreeCellData(Torrent torrent, TorrentFile file, String displayName) {
        this.torrent = torrent;
        this.file = file;
        this.displayName = displayName;
        this.isRoot = false;
    }

    public TorrentTreeCellData(Torrent torrent, boolean isRoot) {
        this.torrent = torrent;
        this.isRoot = isRoot;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
