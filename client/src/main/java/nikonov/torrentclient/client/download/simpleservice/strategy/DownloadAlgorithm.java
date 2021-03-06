package nikonov.torrentclient.client.download.simpleservice.strategy;

import nikonov.torrentclient.client.download.domain.PeerBlockRequest;
import nikonov.torrentclient.client.download.domain.peer.Peer;
import nikonov.torrentclient.client.download.domain.Block;

import java.util.Collection;
import java.util.List;

/**
 * Алгоритм загрузки блоков
 * На вход принимает коллекцию блоков, которые клиену нужно скачать и коллекцию пиров с которыми клиент установил соединение
 * На выходе список пар вида блок - пир, у которого надо запросить этот блок
 */
public interface DownloadAlgorithm {
    List<PeerBlockRequest> downloadBlock(Collection<Block> blocks, Collection<Peer> peers);
}
