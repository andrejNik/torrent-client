package nikonov.torrentclient.client.download.peer;

import nikonov.torrentclient.client.domain.Bitfield;
import nikonov.torrentclient.client.domain.DownloadData;
import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.download.domain.DownloadState;
import nikonov.torrentclient.client.download.domain.Block;
import nikonov.torrentclient.client.download.domain.peer.Peer;
import nikonov.torrentclient.client.download.domain.peer.PeerState;
import nikonov.torrentclient.client.network.NetworkService;
import nikonov.torrentclient.client.network.domain.message.*;
import nikonov.torrentclient.client.util.PeerIdService;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerServiceImpl implements PeerService {

    private final NetworkService networkService;
    private final PeerIdService peerIdService;
    private final DownloadData downloadData;
    private final DownloadState downloadState;
    private final Map<PeerAddress, Peer> peerMap;
    /**
     * Время ожидания перехода из состояния CONNECT в состояние ACTIVE
     * Пир при подключении находится в состояний CONNECT.
     * Как только от пира придет handshake и unchoke сообщение, он переидет в состояние ACTIVE
     */
    public static final Duration ACTIVE_STATE_WAIT_TIME = Duration.of(30, ChronoUnit.SECONDS);

    public PeerServiceImpl(NetworkService networkService,
                           PeerIdService peerIdService,
                           DownloadData downloadData,
                           DownloadState downloadState) {
        this.networkService = networkService;
        this.peerIdService = peerIdService;
        this.downloadData = downloadData;
        this.downloadState = downloadState;
        this.peerMap = new ConcurrentHashMap<>();
    }

    @Override
    public List<Peer> peers() {
        disconnectInactivePeers(); // TODO СДЕЛАТЬ ОТДЕЛЬНЫЙ ПОТОК В ФОНЕ ?
        return peerMap
                .values()
                .stream()
                .filter(peer -> peer.getState() == PeerState.ACTIVE)
                .collect(Collectors.toList());
    }

    @Override
    public void connect(PeerAddress peerAddress) {
        var peer = new Peer(peerAddress);
        peer.setState(PeerState.CONNECT);
        peer.setLastActiveTime(Instant.now());
        peer.setBitfield(new Bitfield(downloadData.getMetadata().countPiece()));
        peerMap.put(peerAddress, peer);
        networkService.send(handshakeMessage(peerAddress));
    }

    @Override
    public void disconnect(PeerAddress peerAddress) {
        peerMap.remove(peerAddress);
    }

    @Override
    public void handshakeMessage(HandshakeMessage handshakeMessage) {
        var peerInfoHash = handshakeMessage.getInfoHash();
        var address = handshakeMessage.getSender();
        var peer = peerMap.get(address);
        if (peer != null && Arrays.equals(peerInfoHash, downloadData.getMetadata().getInfo().getSha1Hash())) {
            peer.setLastActiveTime(Instant.now());
        } else {
            networkService.disconnect(address);
        }
    }

    @Override
    public void bitfieldMessage(BitfieldMessage bitfieldMessage) {
        var peerBitfield = bitfieldMessage.getBitfield();
        var address = bitfieldMessage.getSender();
        var peer = peerMap.get(address);
        if (peer != null) {
            peer.setBitfield(peerBitfield);
            peer.setLastActiveTime(Instant.now());
            if (interest(peer)) {
                peer.setAmInterested(true);
                networkService.send(new InterestedMessage(address));
            }
        } else {
            networkService.disconnect(address);
        }
    }

    @Override
    public void unchokeMessage(UnchokeMessage unchokeMessage) {
        var address = unchokeMessage.getSender();
        var peer = peerMap.get(address);
        if (peer != null) {
            peer.setChoking(false);
            peer.setState(PeerState.ACTIVE);
        } else {
            networkService.disconnect(address);
        }
    }

    @Override
    public void chokeMessage(ChokeMessage chokeMessage) {
        var address = chokeMessage.getSender();
        var peer = peerMap.get(address);
        if (peer != null) {
            peer.setChoking(true);
            peer.setLastActiveTime(Instant.now());
        } else {
            networkService.disconnect(address);
        }
    }

    @Override
    public void haveMessage(HaveMessage haveMessage) {
        var sender = haveMessage.getSender();
        var peer = peerMap.get(sender);
        if (peer != null) {
            peer.getBitfield().have(haveMessage.getPieceIndex());
            if (interest(peer) && !peer.isAmInterested()) {
                peer.setAmInterested(true);
                networkService.send(new InterestedMessage(sender));
            }
        } else {
            networkService.disconnect(sender);
        }
    }

    @Override
    public void pieceDownload(int pieceIndex) {
        for (var peer : peerMap.values()) {
            if (!interest(peer) && peer.isAmInterested()) {
                peer.setAmInterested(false);
                networkService.send(new NotInterestedMessage(peer.getAddress()));
                // TODO ПОСТАТЬ HAVE-СООБЩЕНИЕ
            }
        }
    }

    /*private BitfieldMessage bitfieldMessage(PeerAddress recipient) {
        var message = new BitfieldMessage();
        message.setBitfield(bitfield);
        message.setRecipient(recipient);
        return message;
    }*/

    private HandshakeMessage handshakeMessage(PeerAddress recipient) {
        var message = new HandshakeMessage();
        message.setRecipient(recipient);
        message.setInfoHash(downloadData.getMetadata().getInfo().getSha1Hash());
        message.setPeerId(peerIdService.peerId());
        return message;
    }

    /**
     * Интересен ли пир
     */
    private boolean interest(Peer peer) {
        var downloadIndexPieceSet = downloadState.needDownloadBlocks().stream().map(Block::getIndex).collect(Collectors.toSet());
        for (var pieceIndex : downloadIndexPieceSet) {
            if (peer.getBitfield().isHave(pieceIndex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * разорвать соединение с неактивными пирами
     * неактивным пир считается если он не присылает handshake или unchoke сообщение (первое unchoke сообщение) в течении ACTIVE_STATE_WAIT_TIME
     */
    private void disconnectInactivePeers() {
        var inactivePeers = peerMap
                .values()
                .stream()
                .filter(peer -> peer.getState() == PeerState.CONNECT && peer.getLastActiveTime().plus(ACTIVE_STATE_WAIT_TIME).isBefore(Instant.now()))
                .map(Peer::getAddress)
                .collect(Collectors.toSet());
        inactivePeers.forEach(peerAddress -> {
            peerMap.remove(peerAddress);
            networkService.disconnect(peerAddress);
        });
    }
}
