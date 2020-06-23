package nikonov.torrentclient.client.download.сonsistentservice;

import nikonov.torrentclient.client.download.domain.peer.Peer;
import nikonov.torrentclient.client.download.peer.PeerService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * сервис определения нагрузки на пиры
 */
public class PeerPieceService {
    /**
     * максимальное кол-во единомоментно скачиваемых кусков у одного пира
     */
    public static final int MAX_PIECE_COUNT = 3;
    /**
     * карта вида пир -> множество кусков загружаемых в данный момент у пира
     */
    private final Map<Peer, Set<Integer>> peerPieceMap;

    private final PeerService peerService;

    public PeerPieceService(PeerService peerService) {
        this.peerService = peerService;
        this.peerPieceMap = new HashMap<>();
    }

    /**
     * список доступных пиров для указанного куска
     *
     * пир считается доступным, если:
     * 1. на данный момент у него скачивается не более {@value MAX_PIECE_COUNT} кусков;
     * 2. указанный кусок уже скачивается у данноо пира
     */
    public synchronized List<Peer> availablePeers(int pieceIndex) {
        synchronizeMap();
        return peerPieceMap
                .entrySet()
                .stream()
                .filter(entry -> {
                    var pieceDownloadSet = entry.getValue();
                    return pieceDownloadSet.size() < MAX_PIECE_COUNT || pieceDownloadSet.contains(pieceIndex);
                })
                .map(Map.Entry::getKey)
                .filter(peer -> peer.getBitfield().isHave(pieceIndex))
                .collect(Collectors.toList());
    }

    /**
     * попробовать занять указанный пир
     * @return - true если попытка удалась - теперь будешь считаться что кусок скачивается у данного пира
     */
    public synchronized boolean tryOccupyPeer(Peer peer, int pieceIndex) {
        synchronizeMap();
        if ( peerPieceMap.containsKey(peer) && peerPieceMap.get(peer).size() < MAX_PIECE_COUNT) {
            peerPieceMap.computeIfAbsent(peer, key -> new HashSet<>()).add(pieceIndex);
            return true;
        }
        return false;
    }

    /**
     * кусок не скачивается у данного пира
     */
    public synchronized void freePeer(Peer peer, int pieceIndex) {
        synchronizeMap();
        if (peerPieceMap.containsKey(peer)) {
            peerPieceMap.computeIfAbsent(peer, key -> new HashSet<>()).remove(pieceIndex);
        }
    }

    /**
     * синхронизировать карту с сервисом пиров
     */
    private void synchronizeMap() {
        var availablePeers = peerService.peers();
        var notAvailablePeers = peerPieceMap.keySet().stream().filter(peer -> !availablePeers.contains(peer)).collect(Collectors.toList());
        notAvailablePeers.forEach(peerPieceMap::remove);
        availablePeers.forEach(peer -> {
            if (!peerPieceMap.containsKey(peer)) {
                peerPieceMap.put(peer, new HashSet<>());
            }
        });
    }
}
