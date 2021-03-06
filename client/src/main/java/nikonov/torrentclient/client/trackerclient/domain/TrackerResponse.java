package nikonov.torrentclient.client.trackerclient.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nikonov.torrentclient.client.domain.PeerAddress;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackerResponse {

    private Integer interval;
    private Integer leechers;
    private Integer seeders;
    private String trackerId;
    private List<PeerAddress> peerAddresses;

    public List<PeerAddress> getPeerAddresses() {
        if (peerAddresses == null) {
            peerAddresses = new ArrayList<>();
        }
        return peerAddresses;
    }
}
