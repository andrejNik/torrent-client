package nikonov.torrentclient.network.nio;

import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.event.EventService;
import nikonov.torrentclient.network.NetworkService;
import nikonov.torrentclient.network.domain.event.ConnectToPeerEvent;
import nikonov.torrentclient.network.domain.message.Message;
import nikonov.torrentclient.notification.NotificationService;
import nikonov.torrentclient.notification.domain.Notification;
import nikonov.torrentclient.notification.domain.NotificationType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.channels.SelectionKey.*;
import static java.text.MessageFormat.format;

public class NIONetworkService implements NetworkService {

    private final Selector selector;
    private final Map<PeerAddress, NetworkChannel> channelMap;
    private final EventService eventService;
    private final NotificationService notificationService;
    private boolean run;

    public NIONetworkService(EventService eventService, NotificationService notificationService) {
        try {
            this.selector = Selector.open();
            this.eventService = eventService;
            this.notificationService = notificationService;
            this.channelMap = new ConcurrentHashMap<>();
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    @Override
    public void start() {
        try {
            run = true;
            while (run) {
                var count = selector.select(100);
                if (count > 0) {
                    var keySet = selector.selectedKeys();
                    for (var key : keySet) {
                        var networkChannel = (NetworkChannel) key.attachment();
                        if (key.isValid() && key.isReadable())
                            networkChannel.read();
                        if (key.isValid() && key.isWritable()) {
                            networkChannel.write();
                        }
                    }
                    keySet.clear();
                }
            }
        } catch (IOException ignore) {
            // TODO логирование
            System.exit(-1);
        } finally {
            channelMap.values().forEach(NetworkChannel::close);
            try {
                selector.close();
            } catch (IOException ignore) {
                // TODO логирование
            }
        }
    }

    @Override
    public <T extends Message> void send(T message) {
        var networkChannel = channelMap.get(message.getRecipient());
        if (networkChannel != null) {
            networkChannel.send(message);
        }
    }

    /**
     * Есть проблема с подключением в неблокирующем режиме, поэтому как временное решение используется блокирующее подключение
     * https://stackoverflow.com/questions/6540346/java-solaris-nio-op-connect-problem
     */
    @Override
    public void connect(PeerAddress address) {
        CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName(format("NIONetWorkService connect {0}:{1}", address.getIp(), address.getPort()));
            try {
                var channel = SocketChannel.open();
                channel.connect(new InetSocketAddress(address.getIp(), address.getPort()));
                notificationService.notice(new Notification<>(this.getClass(), NotificationType.CONNECT, new Object[]{address.getIp(), address.getPort()}));
                channel.configureBlocking(false);
                var networkChannel = new NetworkChannel(channel, eventService, address);
                channelMap.put(address, networkChannel);
                channel.register(selector, OP_READ | OP_WRITE, networkChannel);
                eventService.publishEvent(new ConnectToPeerEvent(address)); // FIXME не очень хорошо что оповешение о соединение вышло за пределы NetworkService
            } catch (IOException ignore) {
                // TODO логирование
            }
        });
    }

    @Override
    public void close() {
        run = false;
    }
}
