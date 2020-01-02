package nikonov.torrentclient.trackerclient.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * Запрос списка учасников 
 */
@Setter
@Getter
public class TrackerRequest {

    private TrackerInfo trackerInfo;
    private ClientInfo clientInfo;
    private byte[] infoHash;
    private long downloaded;
    private long left;
    private long uploaded;
    private Event event;
}
