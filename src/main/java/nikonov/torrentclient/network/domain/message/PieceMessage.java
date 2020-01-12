package nikonov.torrentclient.network.domain.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
