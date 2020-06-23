package nikonov.torrentclient.client.metadata.domain.metadata;

import lombok.*;

/**
 * Announce трекера
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TrackerAnnounce {
    /**
     * Протокол трекера
     */
    private TrackerProtocol protocol;
    /**
     * Доменное имя или ip трекера
     */
    private String host;
    /**
     * Порт трекера
     */
    private Integer port;
    /**
     * Дополнительные данные
     * Например в разделе "announce list" почти у всех трекеров добавляется announce - udp://tracker.openbittorrent.com:80/announce
     */
    private String additional;
}
