package nikonov.torrentclient.trackerclient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nikonov.torrentclient.domain.DownloadData;
import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.download.PeerAddressSupplierService;
import nikonov.torrentclient.metadata.domain.metadata.TrackerAnnounce;
import nikonov.torrentclient.metadata.domain.metadata.TrackerProtocol;
import nikonov.torrentclient.trackerclient.domain.*;
import nikonov.torrentclient.util.PeerIdService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.*;

public class TrackerPeerAddressSupplierService implements PeerAddressSupplierService {

    private final DownloadData downloadData;
    private final PeerIdService peerIdService;
    private final Map<TrackerAnnounce, TrackerData> trackerDataMap;
    private final Statistics statistics;
    private final int port;
    private Event event;

    public TrackerPeerAddressSupplierService(DownloadData downloadData, PeerIdService peerIdService, int port) {
        this.downloadData = downloadData;
        this.peerIdService = peerIdService;
        this.port = port;
        this.trackerDataMap = new HashMap<>();
        this.statistics = new Statistics(0, left(), 0);
    }

    @Override
    public Set<PeerAddress> get() {
        var addressSet = new HashSet<PeerAddress>();
        var futureList = new ArrayList<CompletableFuture<Void>>();
        event = event == null ? Event.STARTED : Event.NONE;
        for(var trackerAnnounce : downloadData.getMetadata().getTrackerAnnounces()) {
            if (canSendRequest(trackerAnnounce)) {
                var client = trackerAnnounce.getProtocol() == TrackerProtocol.UDP ? new UDPTrackerClient() : new HTTPTrackerClient();
                futureList.add(CompletableFuture
                        .supplyAsync(() -> client.request( trackerRequest(trackerAnnounce) ))
                        .thenAccept(trackerResponse -> {
                            if (trackerResponse != null) {
                                trackerDataMap.put(trackerAnnounce, new TrackerData(Instant.now(), trackerResponse.getInterval(), null));
                                addressSet.addAll(trackerResponse.getPeerAddresses());
                            }
                        }));
            }
        }
        futureList.forEach(CompletableFuture::join);
        return addressSet;
    }

    @Override
    public void pieceDownload(int pieceLength) {
        statistics.downloaded += pieceLength;
    }

    @Override
    public void pieceUpload(int pieceLength) {
        statistics.uploaded += pieceLength;
    }

    @Override
    public void complete() {

    }

    private boolean canSendRequest(TrackerAnnounce announce) {
        if (trackerDataMap.containsKey(announce)) {
            var trackerData = trackerDataMap.get(announce);
            return Instant.now().isAfter(trackerData.lastRequestTime.plus(trackerData.interval, ChronoUnit.SECONDS));
        }
        return true;
    }

    private TrackerRequest trackerRequest(TrackerAnnounce trackerAnnounce) {
        var trackerRequest = new TrackerRequest();
        trackerRequest.setTrackerInfo(new TrackerInfo(
                trackerAnnounce.getHost(),
                trackerAnnounce.getPort(),
                trackerAnnounce.getAdditional(),
                ofNullable(trackerDataMap.get(trackerAnnounce)).map(TrackerData::getTrackerId).orElse(null))
        );
        trackerRequest.setClientInfo(new ClientInfo(peerIdService.peerId(), port));
        trackerRequest.setInfoHash(downloadData.getMetadata().getInfo().getSha1Hash());
        trackerRequest.setEvent(event);
        trackerRequest.setUploaded(statistics.uploaded);
        trackerRequest.setDownloaded(statistics.downloaded);
        trackerRequest.setLeft(statistics.left);
        return trackerRequest;
    }

    private long left() {
        var left = 0L;
        for(var i = 0; i < downloadData.getMetadata().getInfo().getFiles().size(); i++) {
            if (downloadData.getFileToDownloadIndexes().contains(i)) {
                var fileInfo = downloadData.getMetadata().getInfo().getFiles().get(i);
                left += fileInfo.getLength();
            }
        }
        return left;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TrackerData {
        /**
         * время последнего обращения к трекеру
         */
        private Instant lastRequestTime;
        /**
         *
         */
        private Integer interval;
        /**
         *
         */
        private String trackerId;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class Statistics {
        /**
         * общее число скачанных байт
         */
        private long downloaded;
        /**
         * сколько клиент еще должен скачать
         */
        private long left;
        /**
         * общее число отданных байт
         */
        private long uploaded;
    }
}
