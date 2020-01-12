package nikonov.torrentclient.network.domain.message;

import nikonov.torrentclient.domain.PeerAddress;

public class InterestedMessage extends Message {

    public InterestedMessage() {

    }

    public InterestedMessage(PeerAddress recipient) {
        super(recipient);
    }

    public static byte id() {
        return 2;
    }

    public static int len() {
        return 5;
    }
}
