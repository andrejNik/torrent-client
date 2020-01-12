package nikonov.torrentclient.notification;

import nikonov.torrentclient.notification.domain.Notification;

public interface NotificationService {

    <T> void notice(Notification<T> notification);
}
