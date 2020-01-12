package nikonov.torrentclient.network.domain.event;

import nikonov.torrentclient.domain.PeerAddress;

public class ConnectToPeerEvent extends BaseNetworkEvent {

    public ConnectToPeerEvent(PeerAddress peerAddress) {
        super(peerAddress);
    }
}
