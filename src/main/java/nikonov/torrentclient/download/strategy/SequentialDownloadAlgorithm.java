package nikonov.torrentclient.download.strategy;

import nikonov.torrentclient.download.domain.PeerBlockRequest;
import nikonov.torrentclient.download.domain.peer.Peer;
import nikonov.torrentclient.download.domain.Block;

import java.util.*;

public class SequentialDownloadAlgorithm implements DownloadAlgorithm {

    @Override
    public List<PeerBlockRequest> downloadBlock(Collection<Block> blocks, Collection<Peer> peers) {
        var list = new ArrayList<PeerBlockRequest>();
        var limit = peers.stream().filter(peer -> peer.isAmInterested() && !peer.isChoking()).count();
        var pieceBlockMap = blocks
                .stream()
                .sorted(Comparator.comparingInt(Block::getIndex))
                .collect(
                        LinkedHashMap<Integer, List<Block>>::new,
                        (map, block) -> map.computeIfAbsent(block.getIndex(), index -> new ArrayList<>()).add(block),
                        LinkedHashMap::putAll
                );
        var usedPeers = new HashSet<>();
        for (var pieceIndex : pieceBlockMap.keySet()) {
            for (var peer : peers) {
                if (!peer.isChoking() && peer.getBitfield().isHave(pieceIndex) && !usedPeers.contains(peer)) {
                    for (var pieceBlock : pieceBlockMap.get(pieceIndex)) {
                        list.add(new PeerBlockRequest(peer.getAddress(), pieceBlock));
                    }
                    usedPeers.add(peer);
                    break;
                }
            }
            if (usedPeers.size() == limit) {
                break;
            }
        }
        return list;
    }
}
