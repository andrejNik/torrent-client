package nikonov.torrentclient.download;

import com.google.common.hash.Hashing;
import nikonov.torrentclient.domain.*;
import nikonov.torrentclient.download.domain.DownloadBlock;
import nikonov.torrentclient.download.domain.PeerBlockRequest;
import nikonov.torrentclient.download.domain.Peer;
import nikonov.torrentclient.download.domain.PieceByteBlock;
import nikonov.torrentclient.download.strategy.DownloadAlgorithm;
import nikonov.torrentclient.filestorage.PieceConsumerService;
import nikonov.torrentclient.metadata.domain.metadata.File;
import nikonov.torrentclient.network.NetworkService;
import nikonov.torrentclient.network.domain.message.*;
import nikonov.torrentclient.util.DomainUtil;
import nikonov.torrentclient.util.PeerIdService;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class DownloadServiceImpl implements DownloadService {

    private final NetworkService networkService;
    private final PieceConsumerService pieceConsumerService;
    private final Set<DownloadBlock> downloadBlocks;
    private final Map<Integer, Set<PieceByteBlock>> pieceBlockMap;
    private final Map<PeerAddress, Peer> peerMap;
    private DownloadAlgorithm downloadAlgorithm;
    private final Bitfield bitfield;
    private final DownloadData downloadData;
    private final PeerIdService peerIdService;

    public static final int BLOCK_LENGTH = (int) Math.pow(2, 14);

    /**
     * Периодичность отправки запросов пирам
     */
    public static final int REQUEST_FREQUENCY = 5_000;

    public DownloadServiceImpl(NetworkService networkService,
                               PieceConsumerService pieceConsumerService,
                               DownloadAlgorithm downloadAlgorithm,
                               DownloadData downloadData,
                               PeerIdService peerIdService) {
        this.networkService = networkService;
        this.pieceConsumerService = pieceConsumerService;
        this.downloadData = downloadData;
        this.downloadBlocks = downloadBlockSet();
        this.pieceBlockMap = new ConcurrentHashMap<>();
        this.peerMap = new ConcurrentHashMap<>();
        this.downloadAlgorithm = downloadAlgorithm;
        this.bitfield = new Bitfield(downloadData.getMetadata().countPiece());
        this.peerIdService = peerIdService;
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

    @Override
    public void connect(PeerAddress peerAddress) {
        networkService.send(handshakeMessage(peerAddress));
    }

    @Override
    public void disconnect(PeerAddress peerAddress) {
        peerMap.remove(peerAddress);
    }

    @Override
    public void bitfieldMessage(BitfieldMessage message) {
        var peerBitfield = message.getBitfield();
        var sender = message.getSender();
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setBitfield(peerBitfield);
            if (interest(peer)) {
                peer.setAmInterested(true);
                networkService.send(new InterestedMessage(sender));
            }
        }
    }

    @Override
    public void chokeMessage(ChokeMessage chokeMessage) {
        var sender = chokeMessage.getSender();
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setChoking(true);
        }
    }

    @Override
    public void handshake(HandshakeMessage message) {
        var sender = message.getSender();
        if (Arrays.equals(downloadData.getMetadata().getInfo().getSha1Hash(), message.getInfoHash())) {
            var peer = new Peer(sender);
            peerMap.put(sender, peer);
            //networkService.send(bitfieldMessage(sender));
        }
    }

    @Override
    public void haveMessage(HaveMessage message) {
        var sender = message.getSender();
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.getBitfield().have(message.getPieceIndex());
            if (interest(peer) && !peer.isAmInterested()) {
                peer.setAmInterested(true);
                networkService.send(new InterestedMessage(sender));
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
        pieceBlockMap
                .computeIfAbsent(message.getIndex(), index -> new HashSet<>())
                .add(new PieceByteBlock(message.getBegin(), message.getBlock()));
        downloadBlocks.remove(new DownloadBlock(message.getIndex(), message.getBegin(), message.getBlock().length));
        // fixme можно не перебирать коллекцию а смотреть в pieceBlockMap
        var pieceDownload = downloadBlocks.stream().noneMatch(downloadBlock -> downloadBlock.getIndex() == message.getIndex());
        if (pieceDownload) {
            var piece = pieceBlockMap
                    .get(message.getIndex())
                    .stream()
                    .sorted(Comparator.comparingInt(PieceByteBlock::getBegin))
                    .map(PieceByteBlock::getBlock)
                    .reduce((arr1, arr2) -> {
                        var union = new byte[arr1.length + arr2.length];
                        System.arraycopy(arr1, 0, union, 0, arr1.length);
                        System.arraycopy(arr2, 0, union, arr1.length, arr2.length);
                        return union;
                    })
                    .get();
            if (Arrays.equals(Hashing.sha1().hashBytes(piece).asBytes(), downloadData.getMetadata().getInfo().getPieceHashes()[message.getIndex()])) {
                bitfield.have(message.getIndex());
                pieceConsumerService.apply(message.getIndex(), piece);
                result = true;
                // TODO ПОСЛАТЬ ПИРАМ HAVE СООБЩЕНИЕ
                // TODO ПОСЛЕ ЗАГРУЗКИ КУСКА КЛИЕНТ МОЖЕТ НЕ ИНТЕРЕСОВАТСЯ ОПРЕДЕЛЕННЫМИ ПИРАМИ - ПОСЫЛАТЬ NOT INTERESTED СООБЩЕНИЕ ?
            } else { // кусок скачан с ошибкой - начинаем скачивать заново
                downloadBlocks.addAll(
                        pieceBlockMap
                                .get(message.getIndex())
                                .stream()
                                .map(pieceByteBlock -> new DownloadBlock(message.getIndex(), pieceByteBlock.getBegin(), pieceByteBlock.getBlock().length))
                                .collect(Collectors.toList()));
            }
            pieceBlockMap.remove(message.getIndex());
        }
        return result;
    }

    @Override
    public void unchokeMessage(UnchokeMessage unchokeMessage) {
        var sender = unchokeMessage.getSender();
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setChoking(false);
        }
    }

    private void downloadBlocks() {
        for (var downloadBlock : downloadAlgorithm.downloadBlock(downloadBlocks, peerMap.values())) {
            var peer = peerMap.get(downloadBlock.getAddress());
            if (peer != null) {
                networkService.send(requestMessage(downloadBlock));
            }
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

    private HandshakeMessage handshakeMessage(PeerAddress recipient) {
        var message = new HandshakeMessage();
        message.setRecipient(recipient);
        message.setInfoHash(downloadData.getMetadata().getInfo().getSha1Hash());
        message.setPeerId(peerIdService.peerId());
        return message;
    }

    private BitfieldMessage bitfieldMessage(PeerAddress recipient) {
        var message = new BitfieldMessage();
        message.setBitfield(bitfield);
        message.setRecipient(recipient);
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

    /**
     * Интересен ли пир
     */
    private boolean interest(Peer peer) {
        for (var downloadBlock : downloadBlocks) {
            if (peer.getBitfield().isHave(downloadBlock.getIndex())) {
                return true;
            }
        }
        return false;
    }
}
