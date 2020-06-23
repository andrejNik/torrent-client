package nikonov.torrentclient.client.network.domain.event;

import nikonov.torrentclient.client.domain.PeerAddress;

public class DisconnectEvent extends BaseNetworkEvent {
    public DisconnectEvent(PeerAddress peerAddress) {
        super(peerAddress);
    }
}
