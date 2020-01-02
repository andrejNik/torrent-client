package nikonov.torrentclient.util;

import nikonov.torrentclient.metadata.domain.metadata.Metadata;
import nikonov.torrentclient.metadata.domain.metadata.TrackerProtocol;
import nikonov.torrentclient.trackerclient.domain.ClientInfo;
import nikonov.torrentclient.trackerclient.domain.Event;
import nikonov.torrentclient.trackerclient.domain.TrackerInfo;
import nikonov.torrentclient.trackerclient.domain.TrackerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DomainUtil {

    public static Map<TrackerProtocol, List<TrackerRequest>> requestMap(Metadata meta,
                                                                        ClientInfo clientInfo,
                                                                        long downloaded,
                                                                        long uploaded,
                                                                        Event event) {
        Map<TrackerProtocol, List<TrackerRequest>> map = Map.of(
                TrackerProtocol.UDP,  new ArrayList<TrackerRequest>(),
                TrackerProtocol.HTTP, new ArrayList<TrackerRequest>()
        );
        for(var trackerAnnounce : meta.getTrackerAnnounces()) {
            var request = new TrackerRequest();
            request.setTrackerInfo(new TrackerInfo(trackerAnnounce.getHost(), trackerAnnounce.getPort(), trackerAnnounce.getAdditional(), null));
            request.setClientInfo(clientInfo);
            request.setInfoHash(meta.getInfo().getSha1Hash());
            request.setDownloaded(downloaded);
            request.setUploaded(uploaded);
            request.setEvent(event);
            map.get(trackerAnnounce.getProtocol()).add(request);
        }
        return map;
    }
}
