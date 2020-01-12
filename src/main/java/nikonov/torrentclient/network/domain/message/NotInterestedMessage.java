package nikonov.torrentclient.network.domain.message;

public class NotInterestedMessage extends Message {

    public static byte id() {
        return 3;
    }

    public static int len() {
        return 5;
    }
}
