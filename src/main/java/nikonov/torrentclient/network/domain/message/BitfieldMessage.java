package nikonov.torrentclient.network.domain.message;

import lombok.Getter;
import lombok.Setter;
import nikonov.torrentclient.domain.Bitfield;

@Getter
@Setter
public class BitfieldMessage extends Message {
    private Bitfield bitfield;

    public static byte id() {
        return 5;
    }
}
