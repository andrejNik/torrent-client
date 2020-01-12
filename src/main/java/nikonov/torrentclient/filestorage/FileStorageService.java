package nikonov.torrentclient.filestorage;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import nikonov.torrentclient.domain.DownloadData;
import nikonov.torrentclient.util.DomainUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FileStorageService implements PieceConsumerService, AutoCloseable {

    private final DownloadData downloadData;
    private final Map<Integer, RandomAccessFile> randomAccessFileMap;
    private final Map<Integer, BorderData> fileBorderMap;

    public FileStorageService(DownloadData downloadData) throws IOException {
        this.downloadData = downloadData;
        fileBorderMap = DomainUtil
                .fileBorderMap(downloadData.getMetadata())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new BorderData(entry.getValue()[0], entry.getValue()[1])));
        this.randomAccessFileMap = new HashMap<>();
        for(var fileIndex : downloadData.getFileToDownloadIndexes()) {
            randomAccessFileMap.put(fileIndex, new RandomAccessFile(downloadData.getFileNameMap().get(fileIndex), "rw"));
        }
    }

    @Override
    public void apply(int pieceIndex, byte[] piece) {
        var pieceBorder = pieceBorder(pieceIndex, piece.length);
        for(var fileIndex : downloadData.getFileToDownloadIndexes()) {
            var fileBorder = fileBorderMap.get(fileIndex);
            if (pieceBorder.indexLastByte >= fileBorder.indexFirstByte && pieceBorder.indexLastByte <= fileBorder.indexLastByte) {
                var file = randomAccessFileMap.get(fileIndex);
                synchronized (file) {
                    try {
                        file.seek(seek(fileBorder, pieceBorder));
                        file.write(some(pieceBorder, fileBorder, piece));
                    } catch (IOException ignore) {
                        // TODO логирование
                        closeFile(file);
                    }
                }
            }
        }
    }

    private long seek(BorderData fileBorder, BorderData pieceBorder) {
        var seek = pieceBorder.indexFirstByte - fileBorder.indexFirstByte;
        return seek < 0 ? 0 : seek;
    }

    private byte[] some(BorderData pieceBorder, BorderData fileBorder, byte[] piece) {
        var start = (int)(Math.max(pieceBorder.indexFirstByte, fileBorder.indexFirstByte) - pieceBorder.indexFirstByte);
        var end = (int)(Math.min(pieceBorder.indexLastByte, fileBorder.indexLastByte) - pieceBorder.indexFirstByte);
        if (start == 0 && end + 1 == piece.length) {
            return piece;
        } else {
            return Arrays.copyOfRange(piece, start, end + 1);
        }
    }

    @Override
    public void close() {
        randomAccessFileMap.values().forEach(this::closeFile);
    }

    private void closeFile(RandomAccessFile file) {
        try {
            file.close();
        } catch (IOException ignore) {
            // TODO логирование
        }
    }

    private BorderData pieceBorder(int pieceIndex, int length) {
        return new BorderData(
                pieceIndex * downloadData.getMetadata().getInfo().getPieceLength(),
                pieceIndex * downloadData.getMetadata().getInfo().getPieceLength() + length - 1
        );
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class BorderData {
        private long indexFirstByte;
        private long indexLastByte;
    }
}
