package nikonov.torrentclient.download.piececache;

import nikonov.torrentclient.download.domain.PieceByteBlock;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapPieceCacheService implements PieceCacheService {

    private final Map<Integer, Set<PieceByteBlock>> pieceBlockMap;

    public MapPieceCacheService() {
        pieceBlockMap = new ConcurrentHashMap<>();
    }

    @Override
    public void putPieceBlock(int pieceIndex, PieceByteBlock pieceByteBlock) {
        pieceBlockMap.computeIfAbsent(pieceIndex, index -> new HashSet<>()).add(pieceByteBlock);
    }

    @Override
    public byte[] piece(int pieceIndex) {
        return pieceBlockMap
                .getOrDefault(pieceIndex, new HashSet<>())
                .stream()
                .sorted(Comparator.comparingInt(PieceByteBlock::getBegin))
                .map(PieceByteBlock::getBlock)
                .reduce((arr1, arr2) -> {
                    var union = new byte[arr1.length + arr2.length];
                    System.arraycopy(arr1, 0, union, 0, arr1.length);
                    System.arraycopy(arr2, 0, union, arr1.length, arr2.length);
                    return union;
                })
                .orElseGet(() -> new byte[]{});
    }

    @Override
    public Set<PieceByteBlock> pieceByteBlocks(int pieceIndex) {
        return pieceBlockMap.getOrDefault(pieceIndex, new HashSet<>());
    }

    @Override
    public void removePiece(int pieceIndex) {
        pieceBlockMap.remove(pieceIndex);
    }
}
