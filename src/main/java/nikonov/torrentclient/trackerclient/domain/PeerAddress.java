package nikonov.torrentclient.trackerclient.domain;

import lombok.*;

/**
 * Данные участника раздачи полученные от трекера
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PeerAddress {
    private String ip;
    private int port;
}
