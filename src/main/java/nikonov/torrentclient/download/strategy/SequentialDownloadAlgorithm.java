package nikonov.torrentclient.download.strategy;

import nikonov.torrentclient.download.domain.PeerBlockRequest;
import nikonov.torrentclient.download.domain.Peer;
import nikonov.torrentclient.download.domain.DownloadBlock;

import java.util.*;
import java.util.stream.Collectors;

public class SequentialDownloadAlgorithm implements DownloadAlgorithm {

    @Override
    public List<PeerBlockRequest> downloadBlock(Collection<DownloadBlock> downloadBlocks, Collection<Peer> peers) {
        var list = new ArrayList<PeerBlockRequest>();
        var limit = peers.stream().filter(peer -> peer.isAmInterested() && !peer.isChoking()).count();
        var pieceBlockMap = downloadBlocks
                .stream()
                .sorted(Comparator.comparingInt(DownloadBlock::getIndex))
                .collect(
                        LinkedHashMap<Integer, List<DownloadBlock>>::new,
                        (map, downloadBlock) -> map.computeIfAbsent(downloadBlock.getIndex(), index -> new ArrayList<>()).add(downloadBlock),
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
