package nikonov.torrentclient.network.domain.event;

import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.network.domain.message.Message;

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
