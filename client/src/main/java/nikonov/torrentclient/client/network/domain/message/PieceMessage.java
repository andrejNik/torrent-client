package nikonov.torrentclient.client.network.domain.message;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PieceMessage extends Message {
    private int index;
    private int begin;
    private byte[] block;

    public static byte id() {
        return 7;
    }
}
