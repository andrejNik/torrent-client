package nikonov.torrentclient.download;

import nikonov.torrentclient.download.peersearch.PeerSearchService;
import nikonov.torrentclient.event.EventListener;
import nikonov.torrentclient.network.domain.event.ConnectToPeerEvent;
import nikonov.torrentclient.network.domain.event.DisconnectEvent;
import nikonov.torrentclient.network.domain.event.MessageEvent;
import nikonov.torrentclient.network.domain.message.*;
import nikonov.torrentclient.notification.NotificationService;
import nikonov.torrentclient.notification.domain.Notification;
import nikonov.torrentclient.notification.domain.NotificationType;

public class NetworkEventListener implements EventListener {

    private final DownloadService downloadService;
    private final PeerSearchService peerSearchService;
    private final NotificationService notificationService;

    public NetworkEventListener(DownloadService downloadService,
                                PeerSearchService peerSearchService,
                                NotificationService notificationService) {
        this.downloadService = downloadService;
        this.peerSearchService = peerSearchService;
        this.notificationService = notificationService;
    }

    @Override
    public void handleEvent(Object event) {
        if (event instanceof ConnectToPeerEvent) {
            var address = ((ConnectToPeerEvent)event).getPeerAddress();
            downloadService.connect(address);
            notificationService.notice(new Notification<>(
                    NetworkEventListener.class,
                    NotificationType.SEND_HANDSHAKE_MESSAGE,
                    new Object[]{address.getIp(), address.getPort()})
            );
        }

        if (event instanceof DisconnectEvent) {
            var address = ((DisconnectEvent) event).getPeerAddress();
            downloadService.disconnect(address);
            peerSearchService.inactivePeer(address);
        }

        if (event instanceof MessageEvent) {
            var messageEvent = (MessageEvent) event;

            if (messageEvent.getMessage() instanceof BitfieldMessage) {
                var bitfieldMessage = (BitfieldMessage)messageEvent.getMessage();
                downloadService.bitfieldMessage(bitfieldMessage);
            }

            if (messageEvent.getMessage() instanceof ChokeMessage) {
                var chokeMessage = (ChokeMessage)messageEvent.getMessage();
                downloadService.chokeMessage(chokeMessage);
                notificationService.notice(new Notification<>(
                        NetworkEventListener.class,
                        NotificationType.RECEIVE_CHOKE_MESSAGE,
                        new Object[]{chokeMessage.getSender().getIp(), chokeMessage.getSender().getPort()})
                );
            }

            if (messageEvent.getMessage() instanceof HandshakeMessage) {
                var handshakeMessage = (HandshakeMessage)messageEvent.getMessage();
                downloadService.handshake(handshakeMessage);
                notificationService.notice(new Notification<>(
                        NetworkEventListener.class,
                        NotificationType.RECEIVE_HANDSHAKE_MESSAGE,
                        new Object[]{handshakeMessage.getSender().getIp(), handshakeMessage.getSender().getPort()})
                );
            }

            if (messageEvent.getMessage() instanceof HaveMessage) {
                var haveMessage = (HaveMessage)messageEvent.getMessage();
                downloadService.haveMessage(haveMessage);
            }

            if (messageEvent.getMessage() instanceof PieceMessage) {
                var pieceMessage = (PieceMessage)messageEvent.getMessage();
                var pieceDownload = downloadService.pieceMessage(pieceMessage);
                notificationService.notice(new Notification<>(
                    NetworkEventListener.class,
                    NotificationType.RECEIVE_PIECE_MESSAGE,
                    new Object[]{pieceMessage.getSender().getIp(), pieceMessage.getSender().getPort(), pieceMessage.getIndex(), pieceMessage.getBegin()})
                );
                if (pieceDownload) {
                    peerSearchService.pieceDownload(pieceMessage.getBlock().length);
                    notificationService.notice(new Notification<>(
                            NetworkEventListener.class,
                            NotificationType.PIECE_DOWNLOAD,
                            new Object[]{pieceMessage.getIndex()})
                    );
                }
            }

            if (messageEvent.getMessage() instanceof UnchokeMessage) {
                var unchokeMessage = (UnchokeMessage)messageEvent.getMessage();
                downloadService.unchokeMessage(unchokeMessage);
                notificationService.notice(new Notification<>(
                        NetworkEventListener.class,
                        NotificationType.RECEIVE_UNCHOKE_MESSAGE,
                        new Object[]{unchokeMessage.getSender().getIp(), unchokeMessage.getSender().getPort()})
                );
                peerSearchService.activePeer(unchokeMessage.getSender());
            }
        }
    }
}
