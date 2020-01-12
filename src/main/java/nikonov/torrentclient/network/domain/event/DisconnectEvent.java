package nikonov.torrentclient.network.domain.event;

import nikonov.torrentclient.domain.PeerAddress;

public class DisconnectEvent extends BaseNetworkEvent {
    public DisconnectEvent(PeerAddress peerAddress) {
        super(peerAddress);
    }
}
