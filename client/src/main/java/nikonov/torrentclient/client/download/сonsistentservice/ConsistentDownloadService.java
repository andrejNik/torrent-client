package nikonov.torrentclient.client.download.сonsistentservice;

import nikonov.torrentclient.client.domain.DownloadData;
import nikonov.torrentclient.client.download.DownloadService;
import nikonov.torrentclient.client.download.domain.Block;
import nikonov.torrentclient.client.download.domain.DownloadState;
import nikonov.torrentclient.client.download.peer.PeerService;
import nikonov.torrentclient.client.download.piececache.PieceCacheService;
import nikonov.torrentclient.client.filestorage.PieceConsumerService;
import nikonov.torrentclient.client.network.NetworkService;
import nikonov.torrentclient.client.network.domain.message.PieceMessage;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Сервис с последовательной загрузкой блоков - следующий блок скачивается если получен предыдущий
 * TODO ВЫДЕЛИТЬ АЛГОРИТМ СОЗДАНИЯ/ДОБАВЛЕНИЯ ПРОЦЕССОВ
 */
public class ConsistentDownloadService implements DownloadService {

    private static final Logger logger = Logger.getLogger(ConsistentDownloadService.class.getName());
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("nikonov.torrentclient.logging.message");
    public static final String THREAD_NAME = "Download-Service-Thread";
    public static final Duration DOWNLOAD_SLEEP_DURATION = Duration.ofMillis(500);

    private final PieceConsumerService pieceConsumerService;
    private final PeerService peerService;
    private final NetworkService networkService;
    private final DownloadState downloadState;
    private final DownloadData downloadData;
    private final PieceCacheService pieceCacheService;
    private final PeerPieceService peerPieceService;
    private final Map<Integer, PieceDownloadProcess> pieceDownloadProcessMap;

    public ConsistentDownloadService(NetworkService networkService,
                                     PieceConsumerService pieceConsumerService,
                                     PeerService peerService,
                                     PieceCacheService pieceCacheService,
                                     PeerPieceService peerPieceService,
                                     DownloadData downloadData,
                                     DownloadState downloadState) {
        this.pieceDownloadProcessMap = new ConcurrentHashMap<>();
        this.networkService = networkService;
        this.pieceConsumerService = pieceConsumerService;
        this.peerService = peerService;
        this.pieceCacheService = pieceCacheService;
        this.peerPieceService = peerPieceService;
        this.downloadData = downloadData;
        this.downloadState = downloadState;
    }

    @Override
    public boolean pieceMessage(PieceMessage pieceMessage) {
        var pieceIndex = pieceMessage.getIndex();
        var pieceDownloadProcess = pieceDownloadProcessMap.get(pieceIndex);
        if (pieceDownloadProcess != null) {
            Optional<byte[]> pieceOpt = pieceDownloadProcess.handlePieceMessage(pieceMessage);
            if (pieceOpt.isPresent()) {
                pieceConsumerService.apply(pieceIndex, pieceOpt.get());
                peerService.pieceDownload(pieceIndex);
                pieceDownloadProcessMap.remove(pieceIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public void download() {
        try {
            Thread.currentThread().setName(THREAD_NAME);
            createPieceDownloadProcesses();
            while (!downloadState.allBlockDownloaded()) {
                pieceDownloadProcessMap.values().forEach(PieceDownloadProcess::requestBlockAgainIfNecessary);
                TimeUnit.MILLISECONDS.sleep(DOWNLOAD_SLEEP_DURATION.toMillis());
            }
        } catch (Throwable exp) {
            logger.log(
                    Level.SEVERE,
                    resourceBundle.getString("log.download-service.error"),
                    exp
            );
            System.exit(-1);
        }
    }

    /**
     * TODO ВЫДЕЛИТЬ АЛГОРИТМ СОЗДАНИЯ/ДОБАВЛЕНИЯ ПРОЦЕССОВ
     */
    private void createPieceDownloadProcesses() {
        downloadState
                .needDownloadBlocks()
                .stream()
                .map(Block::getIndex)
                .collect(Collectors.toSet())
                .forEach(index -> {
                    var process = new PieceDownloadProcess(
                            index,
                            networkService,
                            pieceCacheService,
                            peerPieceService,
                            downloadData,
                            downloadState
                    );
                    pieceDownloadProcessMap.put(index, process);
                });
    }
}
