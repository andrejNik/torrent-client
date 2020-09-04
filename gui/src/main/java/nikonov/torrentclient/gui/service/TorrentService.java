package nikonov.torrentclient.gui.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nikonov.torrentclient.base.metadata.TorrentFileParser;
import nikonov.torrentclient.base.metadata.domain.Metadata;
import nikonov.torrentclient.gui.domain.Torrent;
import nikonov.torrentclient.gui.domain.TorrentFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис управления торрентами
 */
public class TorrentService {

    private final Map<Integer, TorrentData> map;

    public TorrentService() {
        map = new HashMap<>();
    }

    /**
     * список существующих торрентов
     */
    public List<Torrent> torrents() {
        return map.values().stream().map(TorrentData::getTorrent).collect(Collectors.toList());
    }

    public Optional<Torrent> torrent(int id) {
        return Optional.ofNullable(map.get(id)).map(TorrentData::getTorrent);
    }

    /**
     * создать новый торрент
     */
    public Torrent create(String pathToTorrentFile) {
        try {
            var meta = new TorrentFileParser().metadata(pathToTorrentFile);
            var torrent = new Torrent();
            torrent.setId(torrentId());
            torrent.setName(Paths.get(pathToTorrentFile).getFileName().toString());
            torrent.setCreationDate(meta.getCreationDate());
            torrent.setCreatedBy(meta.getCreatedBy());
            torrent.setComment(meta.getComment());
            torrent.setFiles(files(meta));
            map.put(torrent.getId(), new TorrentData(torrent, meta));
            return torrent;
        } catch (Exception exp) {
            return null;
        }
    }

    private List<TorrentFile> files(Metadata metaData) {
        var list = new ArrayList<TorrentFile>();
        for(var i = 0; i < metaData.getInfo().getFiles().size(); i++) {
            var metaFile = metaData.getInfo().getFiles().get(i);
            var torrentFile = new TorrentFile();
            torrentFile.setPath(String.join(File.separator, metaFile.getPath()));
            torrentFile.setName(metaFile.getPath().get(metaFile.getPath().size() - 1));
            torrentFile.setIndex(i);
            torrentFile.setLength(metaFile.getLength());
            list.add(torrentFile);
        }
        return list;
    }

    private int torrentId() {
        return map
                .keySet()
                .stream()
                .mapToInt(item -> item)
                .max()
                .orElse(0) + 1;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TorrentData {
        private Torrent torrent;
        private Metadata metaData;
    }
}