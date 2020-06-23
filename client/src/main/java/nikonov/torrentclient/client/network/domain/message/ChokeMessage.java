package nikonov.torrentclient.client.network.domain.message;

public class ChokeMessage extends Message {

    public static byte id() {
        return 0;
    }

    public static int len() {
        return 5;
    }
}
