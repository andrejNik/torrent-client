package nikonov.torrentclient.client.network.domain.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HaveMessage extends Message {
    private int pieceIndex;

    public static byte id() {
        return 4;
    }

    public static int len() {
        return 9;
    }
}
