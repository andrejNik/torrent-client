package nikonov.torrentclient.network.domain.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelMessage extends Message {
    private int index;
    private int begin;
    private int length;

    public static byte id() {
        return 8;
    }

    public static int len() {
        return 15;
    }
}
