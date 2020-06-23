package nikonov.torrentclient.client.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification<T> {
    private Class<T> component;
    private NotificationType type;
    private Object[] data;
}
