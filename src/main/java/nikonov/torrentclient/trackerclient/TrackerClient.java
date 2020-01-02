package nikonov.torrentclient.trackerclient;

import nikonov.torrentclient.metadata.domain.metadata.TrackerProtocol;
import nikonov.torrentclient.trackerclient.domain.TrackerRequest;
import nikonov.torrentclient.trackerclient.domain.TrackerResponse;

/**
 * Интерфейс клиента торрент-трекера
 */
public interface TrackerClient {
    /**
     * Запрос списка участников раздачи
     */
    TrackerResponse request(TrackerRequest request);

    /**
     * Поддерживает ли клиент указанный протокол
     */
    boolean isSupport(TrackerProtocol protocol);
}
