package nikonov.torrentclient.download.domain;

import nikonov.torrentclient.domain.DownloadData;
import nikonov.torrentclient.download.domain.Block;
import nikonov.torrentclient.util.DomainUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Информация о состояний загрузки
 */
public class DownloadState {

    public static final int BLOCK_LENGTH = (int) Math.pow(2, 14); // todo вынести в отдельную константу
    private final DownloadData downloadData;
    private final Set<Block> needDownloadSet;
    /**
     * сколько байт еще нужно скачать
     */
    private long left;
    /**
     * сколько байт скачано
     */
    private long downloaded;

    public DownloadState(DownloadData downloadData) {
        this.downloadData = downloadData;
        this.needDownloadSet = downloadBlockSet();
        this.left = this.needDownloadSet.stream().mapToLong(Block::getLength).sum();
        this.downloaded = 0L;
    }

    /**
     * все блоки скачаны
     */
    public boolean allBlockDownloaded() {
        return needDownloadSet.isEmpty();
    }

    /**
     * блоки, которые необходимо скачать
     */
    public Set<Block> needDownloadBlocks() {
        return Collections.unmodifiableSet( needDownloadSet );
    }

    /**
     * блок загружен
     */
    public void blockDownload(Block block) {
        needDownloadSet.remove(block);
        left -= block.getLength();
        downloaded += block.getLength();
    }

    /**
     * скачан ли кусок
     */
    public boolean pieceDownload(int pieceIndex) {
        return needDownloadSet.stream().noneMatch(block -> block.getIndex() == pieceIndex);
    }

    /**
     * кусок нужно загрузить повторно
     */
    public void downloadAgain(int pieceIndex) {
        var blocks = splitPiece(pieceIndex, downloadData.getMetadata().pieceLength(pieceIndex));
        left += blocks.stream().mapToLong(Block::getLength).sum();
        needDownloadSet.addAll( blocks );
    }

    /**
     * сколько байт клиент еще должен скачать
     */
    public long left() {
        return left;
    }
    /**
     * общее число скачанных байт
     */
    public long downloaded() {
        return downloaded;
    }

    private Set<Block> downloadBlockSet() {
        var map = new HashMap<Integer, List<Block>>();
        var fileBorderMap = DomainUtil.fileBorderMap(downloadData.getMetadata());
        var meta = downloadData.getMetadata();
        for (var indexPiece = 0; indexPiece < meta.countPiece(); indexPiece++) {
            var indexFirstBytePiece = indexPiece * meta.getInfo().getPieceLength();
            var indexLastBytePiece = indexFirstBytePiece + meta.pieceLength(indexPiece) - 1;
            var blocks = splitPiece(indexPiece, meta.pieceLength(indexPiece));
            for (var entry : fileBorderMap.entrySet()) {
                var fileIndex = entry.getKey(); // индекс файла в порядке следования в файле-метаданных
                var indexFirstByteFile = entry.getValue()[0];
                var indexLastByteFile = entry.getValue()[1];
                if (indexLastBytePiece >= indexFirstByteFile && indexLastBytePiece <= indexLastByteFile) {
                    map.computeIfAbsent(fileIndex, key -> new ArrayList<>()).addAll(blocks);
                }
            }
        }
        Set<Block> blocks = ConcurrentHashMap.newKeySet();
        map.entrySet()
                .stream()
                .filter(entry -> downloadData.getFileToDownloadIndexes().contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .forEach(blocks::add);
        return blocks;
    }

    /**
     * Разбить кусок на блоки указанной длины
     *
     * @param pieceIndex  индекс куска
     * @param pieceLength длина куска
     */
    private static List<Block> splitPiece(int pieceIndex, int pieceLength) {
        var list = new ArrayList<Block>();
        var countBlocks = (int) Math.ceil(pieceLength / (double) BLOCK_LENGTH);
        for (var i = 0; i < countBlocks; i++) {
            var begin = i * BLOCK_LENGTH;
            var length = pieceLength - BLOCK_LENGTH * (i + 1) >= 0 ? BLOCK_LENGTH : pieceLength - BLOCK_LENGTH * i;
            list.add(new Block(pieceIndex, begin, length));
        }
        return list;
    }
}
