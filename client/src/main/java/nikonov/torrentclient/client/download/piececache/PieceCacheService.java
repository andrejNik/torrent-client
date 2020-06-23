package nikonov.torrentclient.client.download.piececache;

import nikonov.torrentclient.client.download.domain.PieceByteBlock;

import java.util.Set;

/**
 * кеш блоков загружаемых кусков
 */
public interface PieceCacheService {
    /**
     * добавить блок куска
     */
    void putPieceBlock(int pieceIndex, PieceByteBlock pieceByteBlock);

    /**
     * получить кусок из имеющихся блоков
     */
    byte[] piece(int pieceIndex);

    /**
     * все имеющиеся блоки
     */
    Set<PieceByteBlock> pieceByteBlocks(int pieceIndex);

    /**
     * удалить данные куска
     */
    void removePiece(int pieceIndex);
}
