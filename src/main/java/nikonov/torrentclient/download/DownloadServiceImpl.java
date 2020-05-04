package nikonov.torrentclient.download;

import com.google.common.hash.Hashing;
import nikonov.torrentclient.domain.*;
import nikonov.torrentclient.download.domain.DownloadBlock;
import nikonov.torrentclient.download.domain.PeerBlockRequest;
import nikonov.torrentclient.download.domain.PieceByteBlock;
import nikonov.torrentclient.download.peer.PeerService;
import nikonov.torrentclient.download.piececache.PieceCacheService;
import nikonov.torrentclient.download.strategy.DownloadAlgorithm;
import nikonov.torrentclient.filestorage.PieceConsumerService;
import nikonov.torrentclient.metadata.domain.metadata.File;
import nikonov.torrentclient.network.NetworkService;
import nikonov.torrentclient.network.domain.message.*;
import nikonov.torrentclient.util.DomainUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * TODO 1. выделить сервис кеша кусков
 * TODO 2. парралельная загрузка и блокировка по индексу куска
 *
 */
public class DownloadServiceImpl implements DownloadService {

    private final NetworkService networkService;
    private final PieceConsumerService pieceConsumerService;
    private final Set<DownloadBlock> downloadBlocks;
    private DownloadAlgorithm downloadAlgorithm;
    private final PeerService peerService;
    private final PieceCacheService pieceCacheService;
    private final Bitfield bitfield;
    private final DownloadData downloadData;

    public static final int BLOCK_LENGTH = (int) Math.pow(2, 14);

    /**
     * Периодичность отправки запросов пирам
     */
    public static final int REQUEST_FREQUENCY = 5_000;

    public DownloadServiceImpl(NetworkService networkService,
                               PieceConsumerService pieceConsumerService,
                               PeerService peerService,
                               PieceCacheService pieceCacheService,
                               DownloadAlgorithm downloadAlgorithm,
                               DownloadData downloadData) {
        this.networkService = networkService;
        this.pieceConsumerService = pieceConsumerService;
        this.peerService = peerService;
        this.pieceCacheService = pieceCacheService;
        this.downloadData = downloadData;
        this.downloadBlocks = downloadBlockSet();
        // FIXME возможно стоит заменить на паттерн посредник (между сервисом загрузки и сервисом  пиров)
        peerService.downloadPieceIndexes(downloadBlocks.stream().map(DownloadBlock::getIndex).collect(Collectors.toSet()));
        this.downloadAlgorithm = downloadAlgorithm;
        this.bitfield = new Bitfield(downloadData.getMetadata().countPiece());
    }


    @Override
    public void download() {
        while (!downloadBlocks.isEmpty()) {
            downloadBlocks();
            try {
                Thread.sleep(REQUEST_FREQUENCY);
            } catch (InterruptedException exp) {
                throw new RuntimeException(exp);
            }
        }
    }

    /**
     * FIXME При вызове метода из разных потоков кусок может быть скачан более одного раза
     */
    @Override
    public boolean pieceMessage(PieceMessage message) {
        var result = false;
        if (bitfield.isHave(message.getIndex())) {
            return result;
        }
        pieceCacheService.putPieceBlock(
                message.getIndex(),
                new PieceByteBlock(message.getBegin(), message.getBlock())
        );
        downloadBlocks.remove(new DownloadBlock(message.getIndex(), message.getBegin(), message.getBlock().length));
        // fixme можно не перебирать коллекцию а смотреть в pieceBlockMap
        var pieceDownload = downloadBlocks.stream().noneMatch(downloadBlock -> downloadBlock.getIndex() == message.getIndex());
        if (pieceDownload) {
            var piece = pieceCacheService.piece(message.getIndex());
            if (Arrays.equals(Hashing.sha1().hashBytes(piece).asBytes(), downloadData.getMetadata().getInfo().getPieceHashes()[message.getIndex()])) {
                bitfield.have(message.getIndex());
                pieceConsumerService.apply(message.getIndex(), piece);
                peerService.pieceDownload(message.getIndex());
                result = true;
                // TODO ПОСЛАТЬ ПИРАМ HAVE СООБЩЕНИЕ
                // TODO ПОСЛЕ ЗАГРУЗКИ КУСКА КЛИЕНТ МОЖЕТ НЕ ИНТЕРЕСОВАТСЯ ОПРЕДЕЛЕННЫМИ ПИРАМИ - ПОСЫЛАТЬ NOT INTERESTED СООБЩЕНИЕ ?
            } else { // кусок скачан с ошибкой - начинаем скачивать заново
                downloadBlocks.addAll(
                        // TODO ЛУЧШЕ ИСПОЛЬЗОВАТЬ splitPiece
                        pieceCacheService
                                .pieceByteBlocks(message.getIndex())
                                .stream()
                                .map(pieceByteBlock -> new DownloadBlock(message.getIndex(), pieceByteBlock.getBegin(), pieceByteBlock.getBlock().length))
                                .collect(Collectors.toList()));
            }
            pieceCacheService.removePiece(message.getIndex());
        }
        return result;
    }

    private void downloadBlocks() {
        for (var downloadBlock : downloadAlgorithm.downloadBlock(downloadBlocks, peerService.peers())) {
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

    private Set<DownloadBlock> downloadBlockSet() {
        var map = new HashMap<Integer, List<DownloadBlock>>();
        var pieceLength = downloadData.getMetadata().getInfo().getPieceLength();
        var fileBorderMap = DomainUtil.fileBorderMap(downloadData.getMetadata());
        var summaryLength = downloadData.getMetadata().getInfo().getFiles().stream().mapToLong(File::getLength).sum();
        var lastPieceLength = (summaryLength % pieceLength) == 0 ? pieceLength : (int) (summaryLength % pieceLength);
        var countPiece = (int) Math.ceil(summaryLength / (double) pieceLength);
        for (var indexPiece = 0; indexPiece < countPiece; indexPiece++) {
            var indexFirstBytePiece = indexPiece * pieceLength;
            var indexLastBytePiece = indexFirstBytePiece + (indexPiece != countPiece - 1 ? pieceLength : lastPieceLength) - 1;
            // todo каждый кусок разбивать на блоки необязательно
            var blocks = splitPiece(indexPiece, indexPiece != countPiece - 1 ? pieceLength : lastPieceLength);
            for (var entry : fileBorderMap.entrySet()) {
                var fileIndex = entry.getKey(); // индекс файла в порядке следования в файле-метаданных
                var indexFirstByteFile = entry.getValue()[0];
                var indexLastByteFile = entry.getValue()[1];
                if (indexLastBytePiece >= indexFirstByteFile && indexLastBytePiece <= indexLastByteFile) {
                    map.computeIfAbsent(fileIndex, key -> new ArrayList<>()).addAll(blocks);
                }
            }
        }
        // todo рассмотреть другие способы создания множества
        Set<DownloadBlock> blocks = ConcurrentHashMap.newKeySet();
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
    private static List<DownloadBlock> splitPiece(int pieceIndex, int pieceLength) {
        var list = new ArrayList<DownloadBlock>();
        var countBlocks = (int) Math.ceil(pieceLength / (double) BLOCK_LENGTH);
        for (var i = 0; i < countBlocks; i++) {
            var begin = i * BLOCK_LENGTH;
            var length = pieceLength - BLOCK_LENGTH * (i + 1) >= 0 ? BLOCK_LENGTH : pieceLength - BLOCK_LENGTH * i;
            list.add(new DownloadBlock(pieceIndex, begin, length));
        }
        return list;
    }
}
