package nikonov.torrentclient.network.domain.event;

import nikonov.torrentclient.domain.PeerAddress;

public class BaseNetworkEvent{

    protected PeerAddress peerAddress;

    public BaseNetworkEvent(PeerAddress peerAddress) {
        this.peerAddress = peerAddress;
    }

    public PeerAddress getPeerAddress() {
        return peerAddress;
    }
}
