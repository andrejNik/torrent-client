package nikonov.torrentclient.client.network.domain.message;

import nikonov.torrentclient.client.domain.PeerAddress;

public class NotInterestedMessage extends Message {

    public NotInterestedMessage() {

    }

    public NotInterestedMessage(PeerAddress recipient) {
        super(recipient);
    }

    public static byte id() {
        return 3;
    }

    public static int len() {
        return 5;
    }
}
