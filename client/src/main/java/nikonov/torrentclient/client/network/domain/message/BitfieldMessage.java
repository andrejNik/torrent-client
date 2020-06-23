package nikonov.torrentclient.client.network.domain.message;

import lombok.Getter;
import lombok.Setter;
import nikonov.torrentclient.client.domain.Bitfield;

@Getter
@Setter
public class BitfieldMessage extends Message {
    private Bitfield bitfield;

    public static byte id() {
        return 5;
    }
}
