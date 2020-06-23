package nikonov.torrentclient.client.network.domain.message;

import lombok.Getter;
import lombok.Setter;
import nikonov.torrentclient.client.domain.PeerAddress;

/**
 * Сообщение участника раздачи
 */
@Getter
@Setter
public abstract class Message {
    private PeerAddress sender;
    private PeerAddress recipient;

    public Message() {

    }

    public Message(PeerAddress recipient) {
        this.recipient = recipient;
    }
}
