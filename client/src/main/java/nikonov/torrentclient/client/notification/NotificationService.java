package nikonov.torrentclient.client.notification;

import nikonov.torrentclient.client.notification.domain.Notification;

public interface NotificationService {

    <T> void notice(Notification<T> notification);
}
