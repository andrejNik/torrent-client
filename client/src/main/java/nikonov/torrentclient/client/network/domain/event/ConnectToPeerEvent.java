package nikonov.torrentclient.client.network.domain.event;

import nikonov.torrentclient.client.domain.PeerAddress;

public class ConnectToPeerEvent extends BaseNetworkEvent {

    public ConnectToPeerEvent(PeerAddress peerAddress) {
        super(peerAddress);
    }
}
