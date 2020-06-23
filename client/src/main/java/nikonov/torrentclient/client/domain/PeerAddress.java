package nikonov.torrentclient.client.domain;

import lombok.*;

/**
 * Адрес участника раздачи
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PeerAddress {
    private String ip;
    private int port;
}
