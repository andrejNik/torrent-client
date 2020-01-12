package nikonov.torrentclient.network.domain.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortMessage extends Message{
    private int listenPort;

    public static byte id() {
        return 9;
    }

    public static int len() {
        return 9;
    }
}
