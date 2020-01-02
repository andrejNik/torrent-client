package nikonov.torrentclient.trackerclient.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Данные по трекеру
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackerInfo {
    /**
     * Доменное имя или ip адрес
     */
    private String host;
    /**
     * Прослушиваемый порт
     */
    private Integer port;
    /**
     * Дополнительные данные
     * Например в разделе "announce list" почти у всех трекеров добавляется announce - udp://tracker.openbittorrent.com:80/announce
     */
    private String additional;
    /**
     * (Опционально) Если предыдущее оповещение содержит идентификатор трекера, он должен быть указан тут
     */
    private String trackerId;
}
