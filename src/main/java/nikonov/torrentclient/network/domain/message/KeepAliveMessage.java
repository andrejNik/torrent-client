package nikonov.torrentclient.network.domain.message;

/**
 * Сообщение, посылаемое для сохранения соединения
 */
public class KeepAliveMessage extends Message {

    public static int len() {
        return 4;
    }
}
