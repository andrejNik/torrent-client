package nikonov.torrentclient.client.notification;

import nikonov.torrentclient.client.notification.domain.Notification;


import java.util.ResourceBundle;
import static java.text.MessageFormat.format;

/**
 * TODO проверить что resoueceBundle кеширует строки
 */
public class NotificationServiceImpl implements NotificationService {

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("nikonov.torrentclient.notification.message");

    @Override
    public <T> void notice(Notification<T> notification) {
        String text = null;
        switch (notification.getType()) {
            case CONNECT:
                text = format(resourceBundle.getString("connect"), notification.getData());
                break;
            case NEW_PEERS_DISCOVER:
                text = format(resourceBundle.getString("new-peers-discover"), notification.getData());
                break;
            case SEND_REQUEST_MESSAGE:
                text = format(resourceBundle.getString("send-request-message"), notification.getData());
                break;
            case RECEIVE_HANDSHAKE_MESSAGE:
                text = format(resourceBundle.getString("receive-handshake-message"), notification.getData());
                break;
            case RECEIVE_CHOKE_MESSAGE:
                text = format(resourceBundle.getString("receive-choke-message"), notification.getData());
                break;
            case RECEIVE_UNCHOKE_MESSAGE:
                text = format(resourceBundle.getString("receive-unchoke-message"), notification.getData());
                break;
            case SEND_HANDSHAKE_MESSAGE:
                text = format(resourceBundle.getString("send-handshake-message"), notification.getData());
                break;
            case RECEIVE_PIECE_MESSAGE:
                text = format(resourceBundle.getString("receive-piece-message"), notification.getData());
                break;
            case PIECE_DOWNLOAD:
                text = format(resourceBundle.getString("piece-download"), notification.getData());
                break;
            case PIECE_ERROR_DOWNLOAD:
                text = format(resourceBundle.getString("piece-error-download"), notification.getData());
                break;
        }
        if (text != null) {
            System.out.println(text);
        }
    }
}
