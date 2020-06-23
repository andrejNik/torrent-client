package nikonov.torrentclient.client.network.domain.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestMessage extends Message {
    private int index;
    private int begin;
    private int length;

    public static byte id() {
        return 6;
    }

    public static int len() {
        return 17;
    }
}
