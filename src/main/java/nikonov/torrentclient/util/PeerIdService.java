package nikonov.torrentclient.util;

public class PeerIdService {

    public static final int PEER_ID_LENGTH = 20;

    public byte[] peerId() {
        return new byte[PEER_ID_LENGTH];
    }
}
