package nikonov.torrentclient.download;

import com.google.common.hash.Hashing;
import nikonov.torrentclient.domain.DownloadData;
import nikonov.torrentclient.download.domain.Block;
import nikonov.torrentclient.download.domain.DownloadState;
import nikonov.torrentclient.download.domain.PeerBlockRequest;
import nikonov.torrentclient.download.domain.PieceByteBlock;
import nikonov.torrentclient.download.peer.PeerService;
import nikonov.torrentclient.download.piececache.PieceCacheService;
import nikonov.torrentclient.download.strategy.DownloadAlgorithm;
import nikonov.torrentclient.filestorage.PieceConsumerService;
import nikonov.torrentclient.network.NetworkService;
import nikonov.torrentclient.network.domain.message.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO 1. парралельная загрузка и блокировка по индексу куска
 */
public class DownloadServiceImpl implements DownloadService {

    private static final Logger logger = Logger.getLogger(DownloadServiceImpl.class.getName());
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("nikonov.torrentclient.logging.message");
    private static final String THREAD_NAME = "Download-Service-Thread";

    private final NetworkService networkService;
    private final PieceConsumerService pieceConsumerService;
    private DownloadAlgorithm downloadAlgorithm;
    private final PeerService peerService;
    private final PieceCacheService pieceCacheService;
    private final DownloadState downloadState;
    private final DownloadData downloadData;
    /**
     * Периодичность отправки запросов пирам
     */
    public static final Duration REQUEST_PERIOD = Duration.of(5, ChronoUnit.SECONDS);

    public DownloadServiceImpl(NetworkService networkService,
                               PieceConsumerService pieceConsumerService,
                               PeerService peerService,
                               PieceCacheService pieceCacheService,
                               DownloadAlgorithm downloadAlgorithm,
                               DownloadState downloadState,
                               DownloadData downloadData) {
        this.networkService = networkService;
        this.pieceConsumerService = pieceConsumerService;
        this.peerService = peerService;
        this.pieceCacheService = pieceCacheService;
        this.downloadState = downloadState;
        this.downloadAlgorithm = downloadAlgorithm;
        this.downloadData = downloadData;
    }

    @Override
    public void download() {
        Thread.currentThread().setName(THREAD_NAME);
        Instant lastExecuteTime = null;
        while( !downloadState.allBlockDownloaded() ) {
            if (canExecuteCycleStep(lastExecuteTime)) {
                try {
                    requestBlocks();
                } catch (Exception exp) {
                    logger.log(Level.SEVERE, resourceBundle.getString("log.download-piece.request.error"), exp);
                    System.exit(-1);
                }
                lastExecuteTime = Instant.now();
            }
        }
    }

    /**
     * FIXME При вызове метода из разных потоков кусок может быть скачан более одного раза
     */
    @Override
    public synchronized boolean pieceMessage(PieceMessage message) {
        var result = false;
        var pieceIndex = message.getIndex();
        var begin = message.getBegin();
        var block = message.getBlock();
        if ( downloadState.pieceDownload(pieceIndex) ) {
            return result;
        }
        pieceCacheService.putPieceBlock( pieceIndex, new PieceByteBlock(begin, block) );
        downloadState.blockDownload(new Block(pieceIndex, begin, block.length));
        var pieceDownload = downloadState.pieceDownload(pieceIndex);
        if (pieceDownload) {
            var piece = pieceCacheService.piece(pieceIndex);
            if (pieceHashCorrect(piece, pieceIndex)) {
                pieceConsumerService.apply(pieceIndex, piece);
                peerService.pieceDownload(pieceIndex);
                result = true;
            } else { // кусок скачан с ошибкой - начинаем скачивать заново
                downloadState.downloadAgain(pieceIndex);
            }
            pieceCacheService.removePiece(message.getIndex());
        }
        return result;
    }

    private boolean pieceHashCorrect(byte[] piece, int pieceIndex) {
        return Arrays.equals(
                Hashing.sha1().hashBytes(piece).asBytes(),
                downloadData.getMetadata().getInfo().getPieceHashes()[pieceIndex]
        );
    }

    private boolean canExecuteCycleStep(Instant lastExecuteTime) {
        return lastExecuteTime == null || lastExecuteTime.plus(REQUEST_PERIOD.toSeconds(), ChronoUnit.SECONDS).isBefore(Instant.now());
    }

    private void requestBlocks() {
        var blocks = downloadState.needDownloadBlocks();
        for (var downloadBlock : downloadAlgorithm.downloadBlock(blocks, peerService.peers())) {
            networkService.send(requestMessage(downloadBlock));
        }
    }

    private RequestMessage requestMessage(PeerBlockRequest peerBlockRequest) {
        var message = new RequestMessage();
        message.setRecipient(peerBlockRequest.getAddress());
        message.setIndex(peerBlockRequest.getBlock().getIndex());
        message.setBegin(peerBlockRequest.getBlock().getBegin());
        message.setLength(peerBlockRequest.getBlock().getLength());
        return message;
    }
}
