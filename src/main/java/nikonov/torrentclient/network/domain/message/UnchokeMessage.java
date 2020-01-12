package nikonov.torrentclient.network.domain.message;


public class UnchokeMessage extends Message {

    public static byte id() {
        return 1;
    }

    public static int len() {
        return 5;
    }
}
