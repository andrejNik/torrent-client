package nikonov.torrentclient.client.network.domain.message;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HandshakeMessage extends Message {

    /*
     * FIXME МОЖЕТ БЫТЬ НЕ ОЧЕНЬ ХОРОШАЯ ИДЕЯ ХРАНИТЬ СЕТЕВУЮ ИНФОРМАЦИЮ В КЛАССАХ БИЗНЕС ЛОГИКИ
     */
    public static final byte HANDSHAKE_PSTRLEN = 19;
    public static final String HANDSHAKE_PSTR = "BitTorrent protocol";

    private byte[] infoHash;
    private byte[] peerId;

    public static int len() {
        return 68;
    }
}
