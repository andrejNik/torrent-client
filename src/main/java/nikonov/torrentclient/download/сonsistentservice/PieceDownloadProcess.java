package nikonov.torrentclient.download.сonsistentservice;

import com.google.common.hash.Hashing;
import nikonov.torrentclient.domain.DownloadData;
import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.download.domain.Block;
import nikonov.torrentclient.download.domain.DownloadState;
import nikonov.torrentclient.download.domain.PieceByteBlock;
import nikonov.torrentclient.download.domain.peer.Peer;
import nikonov.torrentclient.download.piececache.PieceCacheService;
import nikonov.torrentclient.network.NetworkService;
import nikonov.torrentclient.network.domain.message.PieceMessage;
import nikonov.torrentclient.network.domain.message.RequestMessage;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * процесс загрузки куска
 */
public class PieceDownloadProcess {
    /**
     * Время ожидания получения блока от пира после запроса
     */
    public static final Duration WAIT_BLOCK_TIME = Duration.of(5, ChronoUnit.SECONDS);
    private final int pieceIndex;
    private final NetworkService networkService;
    private final PieceCacheService pieceCacheService;
    private final PeerPieceService peerPieceService;
    private final DownloadData downloadData;
    private final PieceDownloadData pieceDownloadData;
    private Peer peer;

    public PieceDownloadProcess(int pieceIndex,
                                NetworkService networkService,
                                PieceCacheService pieceCacheService,
                                PeerPieceService peerPieceService,
                                DownloadData downloadData,
                                DownloadState downloadState) {
        this.pieceIndex = pieceIndex;
        this.networkService = networkService;
        this.pieceCacheService = pieceCacheService;
        this.peerPieceService = peerPieceService;
        this.downloadData = downloadData;
        this.pieceDownloadData = new PieceDownloadData(pieceIndex, downloadState);
    }

    /**
     * обработать piece-сообщение от пира
     */
    public synchronized Optional<byte[]> handlePieceMessage(PieceMessage pieceMessage) {
        Optional<byte[]> result = Optional.empty();
        var block = new Block(pieceMessage.getIndex(), pieceMessage.getBegin(), pieceMessage.getBlock().length);
        if (pieceDownloadData.isCurrentBlock(block)) {
            pieceCacheService.putPieceBlock(pieceIndex, new PieceByteBlock(pieceMessage.getBegin(), pieceMessage.getBlock()));
            pieceDownloadData.currentBlockDownload();
            if ( pieceDownloadData.pieceDownload() ) {
                var piece = pieceCacheService.piece(pieceIndex);
                if (pieceHashCorrect(piece, pieceIndex)) {
                    peerPieceService.freePeer(peer, pieceIndex);
                    result = Optional.of(piece);
                } else {
                    pieceDownloadData.downloadAgain();
                }
                pieceCacheService.removePiece(pieceIndex);
            } else { // запрашиваем следующий блок
                requestNextBlock();
            }
        }
        return result;
    }

    /**
     * запросить блок заново, если необходимо
     */
    public synchronized void requestBlockAgainIfNecessary() {
        if ( needRequestAgain() ) {
            request(pieceDownloadData.currentBlock());
        }
    }

    private void requestNextBlock() {
        request( pieceDownloadData.randomBlock() );
    }

    private void request(Block block) {
        updatePeerIfNecessary();
        if (peer != null) {
            networkService.send(requestMessage(peer.getAddress(), block));
            pieceDownloadData.setCurrentBlockAndUpdateTime(block);
            System.out.println("Запрошен блок " + block + " у пира " + peer.getAddress());
        }
    }

    private boolean needRequestAgain() {
        Instant now = Instant.now();
        return pieceDownloadData.currentBlock() != null &&
                pieceDownloadData.time() != null &&
                Duration.between(pieceDownloadData.time(), now).toMillis() > WAIT_BLOCK_TIME.toMillis();
    }

    private void updatePeerIfNecessary() {
        List<Peer> peers = peerPieceService.availablePeers(pieceIndex);
        if (peers.size() == 0 && peer != null) {
            peer = null;
        }
        if (peers.size() != 0 && (peer == null || !peers.contains(peer))) {
            var randomIndex = new Random().nextInt(peers.size());
            if (peerPieceService.tryOccupyPeer(peers.get(randomIndex), pieceIndex)) {
                peer = peers.get(randomIndex);
            }
        }
    }


    private boolean pieceHashCorrect(byte[] piece, int pieceIndex) {
        return Arrays.equals(
                Hashing.sha1().hashBytes(piece).asBytes(),
                downloadData.getMetadata().getInfo().getPieceHashes()[pieceIndex]
        );
    }

    private RequestMessage requestMessage(PeerAddress peerAddress, Block block) {
        var message = new RequestMessage();
        message.setRecipient(peerAddress);
        message.setIndex(block.getIndex());
        message.setBegin(block.getBegin());
        message.setLength(block.getLength());
        return message;
    }

    /**
     * Состояние загрузки куска
     */
    private static class PieceDownloadData {
        /**
         * индекс куска
         */
        private final int pieceIndex;
        /**
         * текущий запрашиваемый блок
         * ( блок был запрошен но ответ от пира не получен )
         */
        private Block currentBlock;
        /**
         * время запроса блока
         */
        private Instant time;

        private final DownloadState downloadState;

        public Block currentBlock() {
            return currentBlock;
        }

        public Instant time() {
            return time;
        }

        public PieceDownloadData(int pieceIndex, DownloadState downloadState) {
            this.pieceIndex = pieceIndex;
            this.downloadState = downloadState;
            this.currentBlock = randomBlock();
            this.time = Instant.now();
        }

        public void downloadAgain() {
            downloadState.downloadAgain(pieceIndex);
        }

        public boolean isCurrentBlock(Block block) {
            return currentBlock != null && currentBlock.equals(block);
        }

        public void currentBlockDownload() {
            if (currentBlock != null) {
                downloadState.blockDownload(currentBlock);
            }
        }

        public boolean pieceDownload() {
            return downloadState.pieceDownload(pieceIndex);
        }

        public Block randomBlock() {
            return downloadState
                    .needDownloadBlocks()
                    .stream()
                    .filter(block -> block.getIndex() == pieceIndex)
                    .findAny()
                    .orElseThrow(RuntimeException::new);
        }

        public void setCurrentBlockAndUpdateTime(Block block) {
            currentBlock = block;
            time = Instant.now();
        }
    }
}
