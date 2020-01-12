package nikonov.torrentclient.download;

import com.google.common.hash.Hashing;
import nikonov.torrentclient.domain.*;
import nikonov.torrentclient.download.domain.DownloadBlock;
import nikonov.torrentclient.download.domain.PeerBlockRequest;
import nikonov.torrentclient.download.domain.Peer;
import nikonov.torrentclient.download.domain.PieceByteBlock;
import nikonov.torrentclient.download.strategy.DownloadAlgorithm;
import nikonov.torrentclient.event.EventListener;
import nikonov.torrentclient.filestorage.PieceConsumerService;
import nikonov.torrentclient.metadata.domain.metadata.File;
import nikonov.torrentclient.network.NetworkService;
import nikonov.torrentclient.network.domain.event.ConnectToPeerEvent;
import nikonov.torrentclient.network.domain.event.DisconnectEvent;
import nikonov.torrentclient.network.domain.event.MessageEvent;
import nikonov.torrentclient.network.domain.message.*;
import nikonov.torrentclient.notification.NotificationService;
import nikonov.torrentclient.notification.domain.Notification;
import nikonov.torrentclient.notification.domain.NotificationType;
import nikonov.torrentclient.util.DomainUtil;
import nikonov.torrentclient.util.PeerIdService;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * TODO: 1. Выделить обработчик сообщений ?
 * TODO: 2. Перенести управление загрузкой в алгоритм (или выделить в компонент), а в сервисе оставить только запросы
 * TODO: 3. ПОИСК НОВЫХ ПИРОВ ВЫНЕСТИ В ОТДЕЛЬНЫЙ СЕРВИС
 */
public class DownloadService implements EventListener {

    private final NetworkService networkService;
    private final PieceConsumerService pieceConsumerService;
    private final PeerAddressSupplierService peerSupplierService;
    private final Set<DownloadBlock> downloadBlocks;
    private final Map<Integer, Set<PieceByteBlock>> pieceBlockMap;
    private final Map<PeerAddress, Peer> peerMap;
    private DownloadAlgorithm downloadAlgorithm;
    private final Bitfield bitfield;
    private final DownloadData downloadData;
    private final PeerIdService peerIdService;
    private final NotificationService notificationService;

    /**
     * Минимальное кол-во пиров которые не блокируют клиента и у которых имеются нужные куски.
     * Если таких пиров меньше будет производиться поиск и подключение к новым
     */
    public static final int MIN_ACTIVE_PEERS = 3;

    public static final int BLOCK_LENGTH = (int) Math.pow(2, 14);

    /**
     * Периодичность отправки запросов пирам
     */
    public static final int REQUEST_FREQUENCY = 5_000;

    public DownloadService(NetworkService networkService,
                           PieceConsumerService pieceConsumerService,
                           PeerAddressSupplierService peerSupplierService,
                           DownloadAlgorithm downloadAlgorithm,
                           DownloadData downloadData,
                           PeerIdService peerIdService,
                           NotificationService notificationService) {
        this.networkService = networkService;
        this.pieceConsumerService = pieceConsumerService;
        this.peerSupplierService = peerSupplierService;
        this.downloadData = downloadData;
        this.downloadBlocks = downloadBlockSet();
        this.pieceBlockMap = new ConcurrentHashMap<>();
        this.peerMap = new ConcurrentHashMap<>();
        this.downloadAlgorithm = downloadAlgorithm;
        this.bitfield = new Bitfield(downloadData.getMetadata().countPiece());
        this.peerIdService = peerIdService;
        this.notificationService = notificationService;
    }

    @Override
    public void handleEvent(Object event) {
        if (event instanceof ConnectToPeerEvent) {
            handleConnectToPeer(((ConnectToPeerEvent) event).getPeerAddress());
        }

        if (event instanceof DisconnectEvent) {
            handleDisconnect(((DisconnectEvent) event).getPeerAddress());
        }

        if (event instanceof MessageEvent) {
            var messageEvent = (MessageEvent) event;

            if (messageEvent.getMessage() instanceof BitfieldMessage) {
                handleBitfieldMessage((BitfieldMessage) messageEvent.getMessage(), messageEvent.getPeerAddress());
            }

            if (messageEvent.getMessage() instanceof CancelMessage) {

            }

            if (messageEvent.getMessage() instanceof ChokeMessage) {
                handleChokeMessage(messageEvent.getPeerAddress());
            }

            if (messageEvent.getMessage() instanceof HandshakeMessage) {
                handleHandshakeMessage((HandshakeMessage) messageEvent.getMessage(), messageEvent.getPeerAddress());
            }

            if (messageEvent.getMessage() instanceof HaveMessage) {
                handleHaveMessage((HaveMessage) messageEvent.getMessage(), messageEvent.getPeerAddress());
            }

            if (messageEvent.getMessage() instanceof InterestedMessage) {
                handleInterestedMessage(messageEvent.getPeerAddress());
            }

            if (messageEvent.getMessage() instanceof KeepAliveMessage) {

            }

            if (messageEvent.getMessage() instanceof NotInterestedMessage) {
                handleNotInterestedMessage(messageEvent.getPeerAddress());
            }

            if (messageEvent.getMessage() instanceof PieceMessage) {
                handlePieceMessage((PieceMessage) messageEvent.getMessage());
            }

            if (messageEvent.getMessage() instanceof PortMessage) {

            }

            if (messageEvent.getMessage() instanceof RequestMessage) {

            }

            if (messageEvent.getMessage() instanceof UnchokeMessage) {
                handleUnchokeMessage(messageEvent.getPeerAddress());
            }
        }
    }

    public void download() {
        while (!downloadBlocks.isEmpty()) {
            connectToNewPeersIfNessesary();
            downloadBlocks();
            try {
                Thread.sleep(REQUEST_FREQUENCY);
            } catch (InterruptedException exp) {
                throw new RuntimeException(exp);
            }
        }
    }

    private void downloadBlocks() {
        var map = new HashMap<PeerAddress, Set<Integer>>();
        for (var downloadBlock : downloadAlgorithm.downloadBlock(downloadBlocks, peerMap.values())) {
            var peer = peerMap.get(downloadBlock.getAddress());
            if (peer != null) {
                if (!map.computeIfAbsent(downloadBlock.getAddress(), address -> new HashSet<>()).contains(downloadBlock.getBlock().getIndex())) {
                    map.get(downloadBlock.getAddress()).add(downloadBlock.getBlock().getIndex());
                    notificationService.notice(new Notification<>(DownloadService.class, NotificationType.SEND_REQUEST_MESSAGE, new Object[]{downloadBlock.getBlock().getIndex(), downloadBlock.getAddress().getIp(), downloadBlock.getAddress().getPort()}));
                }
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

    /**
     * TODO 3
     */
    private void connectToNewPeersIfNessesary() {
        if (activePeers() < MIN_ACTIVE_PEERS) {
            var list = peerSupplierService.get().stream().filter(peer -> !peerMap.containsKey(peer)).collect(Collectors.toList());
            if (list.size() != 0) {
                notificationService.notice(new Notification<>(this.getClass(), NotificationType.NEW_PEERS_DISCOVER, new Object[]{list.size()}));
            }
            list.forEach(networkService::connect);
        }
    }

    private int activePeers() {
        return (int) peerMap.values().stream().filter(peer -> peer.isAmInterested() && !peer.isChoking()).count();
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

    private void handleConnectToPeer(PeerAddress peerAddress) {
        notificationService.notice(new Notification<>(DownloadService.class, NotificationType.SEND_HANDSHAKE_MESSAGE, new Object[]{peerAddress.getIp(), peerAddress.getPort()}));
        networkService.send(handshakeMessage(peerAddress));
    }

    private void handleDisconnect(PeerAddress peerAddress) {
        peerMap.remove(peerAddress);
    }

    private void handleBitfieldMessage(BitfieldMessage message, PeerAddress sender) {
        var peerBitfield = message.getBitfield();
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setBitfield(peerBitfield);
            if (interest(peer)) {
                peer.setAmInterested(true);
                networkService.send(new InterestedMessage(sender));
            }
        }
    }

    private void handleChokeMessage(PeerAddress sender) {
        notificationService.notice(new Notification<>(DownloadService.class, NotificationType.RECEIVE_CHOKE_MESSAGE, new Object[]{sender.getIp(), sender.getPort()}));
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setChoking(true);
        }
    }

    private void handleHandshakeMessage(HandshakeMessage message, PeerAddress sender) {
        if (Arrays.equals(downloadData.getMetadata().getInfo().getSha1Hash(), message.getInfoHash())) {
            notificationService.notice(new Notification<>(DownloadService.class, NotificationType.RECEIVE_HANDSHAKE_MESSAGE, new Object[]{sender.getIp(), sender.getPort()}));
            var peer = new Peer(sender);
            peerMap.put(sender, peer);
            //networkService.send(bitfieldMessage(sender));
        }
    }

    private void handleHaveMessage(HaveMessage message, PeerAddress sender) {
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.getBitfield().have(message.getPieceIndex());
            if (interest(peer) && !peer.isAmInterested()) {
                peer.setAmInterested(true);
                networkService.send(new InterestedMessage(sender));
            }
        }
    }

    private void handleInterestedMessage(PeerAddress sender) {
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setInterested(true);
        }
    }

    private void handleNotInterestedMessage(PeerAddress sender) {
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setInterested(false);
        }
    }

    /**
     * FIXME При вызове метода из разных потоков кусок может быть скачан более одного раза
     */
    private void handlePieceMessage(PieceMessage message) {
        notificationService.notice(new Notification<>(
                DownloadService.class,
                NotificationType.RECEIVE_PIECE_MESSAGE,
                new Object[]{message.getSender().getIp(), message.getSender().getPort(), message.getIndex(), message.getBegin()}));
        if (bitfield.isHave(message.getIndex())) {
            return;
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
                notificationService.notice(new Notification<>(DownloadService.class, NotificationType.PIECE_DOWNLOAD, new Object[]{message.getIndex()}));
                bitfield.have(message.getIndex());
                pieceConsumerService.apply(message.getIndex(), piece);
                peerSupplierService.pieceDownload(piece.length);
                // TODO ПОСЛАТЬ ПИРАМ HAVE СООБЩЕНИЕ
                // TODO ПОСЛЕ ЗАГРУЗКИ КУСКА КЛИЕНТ МОЖЕТ НЕ ИНТЕРЕСОВАТСЯ ОПРЕДЕЛЕННЫМИ ПИРАМИ - ПОСЫЛАТЬ NOT INTERESTED СООБЩЕНИЕ ?
            } else { // кусок скачан с ошибкой - начинаем скачивать заново
                notificationService.notice(new Notification<>(DownloadService.class, NotificationType.PIECE_ERROR_DOWNLOAD, new Object[]{message.getIndex()}));
                downloadBlocks.addAll(
                        pieceBlockMap
                                .get(message.getIndex())
                                .stream()
                                .map(pieceByteBlock -> new DownloadBlock(message.getIndex(), pieceByteBlock.getBegin(), pieceByteBlock.getBlock().length))
                                .collect(Collectors.toList()));
            }
            pieceBlockMap.remove(message.getIndex());
        }
    }

    private void handleUnchokeMessage(PeerAddress sender) {
        notificationService.notice(new Notification<>(DownloadService.class, NotificationType.RECEIVE_UNCHOKE_MESSAGE, new Object[]{sender.getIp(), sender.getPort()}));
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.setChoking(false);
        }
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
        for(var downloadBlock : downloadBlocks) {
            if (peer.getBitfield().isHave(downloadBlock.getIndex())) {
                return true;
            }
        }
        return false;
    }
}
