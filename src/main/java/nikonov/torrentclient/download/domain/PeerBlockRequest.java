package nikonov.torrentclient.download.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nikonov.torrentclient.domain.PeerAddress;

/**
 * Запрос блока у пира
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PeerBlockRequest {
    private PeerAddress address;
    private Block block;
}
