package nikonov.torrentclient.client.network.domain.event;

import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.network.domain.message.Message;

public class MessageEvent extends BaseNetworkEvent {
    private Message message;

    public MessageEvent(PeerAddress peerAddress, Message message) {
        super(peerAddress);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
