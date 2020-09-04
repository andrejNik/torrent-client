package nikonov.torrentclient.base.metadata;

import com.google.common.hash.Hashing;
import nikonov.torrentclient.base.metadata.domain.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.*;
import static nikonov.torrentclient.base.metadata.domain.TorrentFileKey.*;

public class TorrentFileParser {

    private final static Pattern ANNOUNCE_PATTERN = Pattern.compile("(?<protocol>[a-z]+)://(?<host>[^:/]+)(?<port>:\\d+)?(?<additional>/.*)?");

    public Metadata metadata(String path) throws Exception {
        var byteContent = Files.readAllBytes(Paths.get(path));
        var metaMap = (Map<String, BencodeItem>)new BencodeReader(byteContent).content().get(0).getValue();
        var metadata = new Metadata();
        ofNullable(metaMap.get(ANNOUNCE)).map(this::announce).ifPresent(trackerAnnounce -> metadata.getTrackerAnnounces().add(trackerAnnounce));
        ofNullable(metaMap.get(ANNOUNCE_LIST)).map(this::announceList).ifPresent(announces -> metadata.getTrackerAnnounces().addAll(announces));
        ofNullable(metaMap.get(CREATION_DATE)).map(item -> Instant.ofEpochSecond((Long)item.getValue())).ifPresent(metadata::setCreationDate);
        ofNullable(metaMap.get(COMMENT)).map(item -> (String)item.getValue()).ifPresent(metadata::setComment);
        ofNullable(metaMap.get(CREATED_BY)).map(item -> (String)item.getValue()).ifPresent(metadata::setCreatedBy);
        metadata.setInfo(info(metaMap.get(INFO), byteContent));
        return metadata;
    }

    private List<TrackerAnnounce> announceList(BencodeItem bencodeItem) {
        var announces = new ArrayList<TrackerAnnounce>();
        for(var itemList : (List<BencodeItem>)bencodeItem.getValue()) {
            for(var item : (List<BencodeItem>)itemList.getValue()) {
                announces.add(announce(item));
            }
        }
        return announces;
    }

    private TrackerAnnounce announce(BencodeItem bencodeItem) {
        var matcher = ANNOUNCE_PATTERN.matcher((String)bencodeItem.getValue());
        if (matcher.find()) {
            var announce = new TrackerAnnounce();
            announce.setProtocol(TrackerProtocol.valueOf(matcher.group("protocol").toUpperCase()));
            announce.setHost(matcher.group("host"));
            ofNullable(matcher.group("port")).map(port -> Integer.valueOf(port.substring(1))).ifPresent(announce::setPort);
            announce.setAdditional(matcher.group("additional"));
            return announce;
        }
        return null;
    }

    private Info info(BencodeItem infoBencode, byte[] byteContent) {
        var info = new Info();
        var infoMeta = (Map<String, BencodeItem>)infoBencode.getValue();
        info.setPieceLength(((Long)infoMeta.get(INFO_PIECE_LENGTH).getValue()).intValue());
        // важно для строк indexStart указывает на начало значения строки
        info.setPieceHashes(pieceHashes(Arrays.copyOfRange(byteContent, infoMeta.get(INFO_PIECES).getIndexStart(), infoMeta.get(INFO_PIECES).getIndexEnd() + 1)));
        info.setFiles(files(infoMeta));
        // важно что для словаря indexStart указывает на 'd'
        var infoBytes = Arrays.copyOfRange(byteContent, infoBencode.getIndexStart(), infoBencode.getIndexEnd() + 1);
        info.setSha1Hash(Hashing.sha1().hashBytes(infoBytes).asBytes());
        return info;
    }

    private byte[][] pieceHashes(byte[] array) {
        var hashes = new byte[array.length / 20][];
        for(var i = 0; i < hashes.length;i++) {
            hashes[i] = Arrays.copyOfRange(array, i * 20, (i * 20) + 20);
        }
        return hashes;
    }

    private List<File> files(Map<String, BencodeItem> infoMeta) {
        var files = new ArrayList<File>();
        var name = (String)infoMeta.get(INFO_NAME).getValue();
        if(infoMeta.containsKey(INFO_FILES)) { // торрент многофайловый
            for(var item : (List<BencodeItem>)infoMeta.get(INFO_FILES).getValue()) {
                files.add(file((Map<String, BencodeItem>)item.getValue(), name));
            }
        } else {
            files.add(new File((long)infoMeta.get(INFO_LENGTH).getValue(), Collections.singletonList(name)));
        }
        return files;
    }

    private File file(Map<String, BencodeItem> fileMeta, String rootDirectory) {
        var file = new File();
        var paths = (List<BencodeItem>)fileMeta.get(INFO_PATH).getValue();
        file.getPath().add(rootDirectory);
        file.getPath().addAll(paths.stream().map(item -> (String)item.getValue()).collect(Collectors.toList()));
        file.setLength((long)fileMeta.get(INFO_LENGTH).getValue());
        return file;
    }
}
