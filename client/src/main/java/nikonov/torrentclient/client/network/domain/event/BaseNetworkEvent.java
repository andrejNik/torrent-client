package nikonov.torrentclient.client.network.domain.event;

import nikonov.torrentclient.client.domain.PeerAddress;

public class BaseNetworkEvent{

    protected PeerAddress peerAddress;

    public BaseNetworkEvent(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() {
        return peerAddress;
    }
}
