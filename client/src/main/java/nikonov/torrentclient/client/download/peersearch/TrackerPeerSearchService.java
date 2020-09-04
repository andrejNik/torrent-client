package nikonov.torrentclient.client.download.peersearch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nikonov.torrentclient.client.domain.DownloadData;
import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.download.domain.DownloadState;
import nikonov.torrentclient.client.download.peer.PeerService;
import nikonov.torrentclient.base.metadata.domain.TrackerAnnounce;
import nikonov.torrentclient.base.metadata.domain.TrackerProtocol;
import nikonov.torrentclient.client.network.NetworkService;
import nikonov.torrentclient.client.notification.NotificationService;
import nikonov.torrentclient.client.notification.domain.Notification;
import nikonov.torrentclient.client.notification.domain.NotificationType;
import nikonov.torrentclient.client.trackerclient.HTTPTrackerClient;
import nikonov.torrentclient.client.trackerclient.UDPTrackerClient;
import nikonov.torrentclient.client.trackerclient.domain.*;
import nikonov.torrentclient.client.util.PeerIdService;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Optional.*;

public class TrackerPeerSearchService implements PeerSearchService {

    private static final Logger logger = Logger.getLogger(TrackerPeerSearchService.class.getName());
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("nikonov.torrentclient.logging.message");
    private final NetworkService networkService;
    private final NotificationService notificationService;
    private final DownloadData downloadData;
    private final DownloadState downloadState;
    private final PeerIdService peerIdService;
    private final Map<TrackerAnnounce, TrackerData> trackerDataMap;
    private final int port;
    private Event event;
    private volatile boolean run;
    private final PeerService peerService;
    private final Set<PeerAddress> discoverPeerSet;

    /**
     * Минимальное кол-во активных пиров
     */
    public static final int MIN_ACTIVE_PEERS = 5;

    /**
     * период отправки запросов к трекерам
     */
    public static final Duration SEARCH_PERIOD = Duration.of(5, ChronoUnit.SECONDS);

    public static final String THREAD_NAME = "Tracker-Peer-Search-Service-Thread";

    public TrackerPeerSearchService(NetworkService networkService,
                                    NotificationService notificationService,
                                    PeerService peerService,
                                    DownloadData downloadData,
                                    DownloadState downloadState,
                                    PeerIdService peerIdService,
                                    int port) {
        this.networkService = networkService;
        this.notificationService = notificationService;
        this.peerService = peerService;
        this.downloadData = downloadData;
        this.downloadState = downloadState;
        this.peerIdService = peerIdService;
        this.port = port;
        this.trackerDataMap = new HashMap<>();
        this.discoverPeerSet = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void start() {
        Thread.currentThread().setName(THREAD_NAME);
        Instant lastExecuteTime = null;
        run = true;
        while (run) {
            if (canExecuteCycleStep(lastExecuteTime)) {
                try {
                    searchAndConnectToNewPeersIfNecessary();
                } catch (Exception exp) {
                    logger.log(Level.SEVERE, resourceBundle.getString("log.tracker-peer-search.error"), exp);
                }
                lastExecuteTime = Instant.now();
            }
        }
    }

    @Override
    public void stop() {
        run = false;
    }

    @Override
    public void complete() {

    }

    @Override
    public void disconnect(PeerAddress peerAddress) {
        discoverPeerSet.remove(peerAddress);
    }

    private boolean canExecuteCycleStep(Instant lastExecuteTime) {
        return lastExecuteTime == null || lastExecuteTime.plus(SEARCH_PERIOD.toSeconds(), ChronoUnit.SECONDS).isBefore(Instant.now());
    }

    private void searchAndConnectToNewPeersIfNecessary() {
        if (activePeers() < MIN_ACTIVE_PEERS) {
            var newPeers = searchPeers()
                    .stream()
                    .filter(address -> !discoverPeerSet.contains(address))
                    .collect(Collectors.toSet());
            if (newPeers.size() != 0) {
                discoverPeerSet.addAll(newPeers);
                notificationService.notice(new Notification<>(this.getClass(), NotificationType.NEW_PEERS_DISCOVER, new Object[]{newPeers.size()}));
                newPeers.forEach(networkService::connect);
            }
        }
    }

    private int activePeers() {
        return (int) peerService
                .peers()
                .stream()
                .filter(peer -> peer.isAmInterested() && !peer.isChoking())
                .count();
    }

    private Set<PeerAddress> searchPeers() {
        var addressSet = new HashSet<PeerAddress>();
        var futureList = new ArrayList<CompletableFuture<Void>>();
        event = event == null ? Event.STARTED : Event.NONE;
        for (var trackerAnnounce : downloadData.getMetadata().getTrackerAnnounces()) {
            if (canSendRequest(trackerAnnounce)) {
                var client = trackerAnnounce.getProtocol() == TrackerProtocol.UDP ? new UDPTrackerClient() : new HTTPTrackerClient();
                futureList.add(CompletableFuture
                        .supplyAsync(() -> client.request(trackerRequest(trackerAnnounce)))
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
        trackerRequest.setUploaded(0L); // FIXME ПОКА ОТДАЧИ НЕТ
        trackerRequest.setDownloaded(downloadState.downloaded());
        trackerRequest.setLeft(downloadState.left());
        return trackerRequest;
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
}
