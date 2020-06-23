package nikonov.torrentclient.client.trackerclient.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Данные о клиенте для трекера
 */
@Getter
@Setter
@NoArgsConstructor
public class ClientInfo {
    public static final int DEFAULT_KEY = 0;
    /**
     * Уникальный идентификатор клиента
     */
    private byte[] peerId;
    /**
     * (Опционально) Реальный ip-адрес клиента
     */
    private String ip;
    /**
     * Номер прослушиваемого клиентом порта
     */
    private int port;
    /**
     * (Опционально) Дополнительная идентификация клиента
     */
    private Integer key;

    public ClientInfo(byte[] peerId, int port) {
        this.peerId = peerId;
        this.port = port;
    }
}
