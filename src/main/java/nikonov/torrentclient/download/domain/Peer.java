package nikonov.torrentclient.download.domain;

import lombok.Getter;
import lombok.Setter;
import nikonov.torrentclient.domain.Bitfield;
import nikonov.torrentclient.domain.PeerAddress;

import java.util.Objects;

@Getter
@Setter
public class Peer {

    private PeerAddress address;
    private Bitfield bitfield;
    private boolean choking;
    private boolean interested;

    private boolean amChoking;
    private boolean amInterested;

    public Peer(PeerAddress address) {
        this.address = address;
        choking = true;
        interested = false;
        amChoking = false;
        amInterested = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(address, peer.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
