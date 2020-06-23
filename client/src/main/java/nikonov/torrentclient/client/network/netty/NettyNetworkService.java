package nikonov.torrentclient.client.network.netty;

import io.netty.channel.nio.NioEventLoopGroup;
import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.event.EventService;
import nikonov.torrentclient.client.network.NetworkService;
import nikonov.torrentclient.client.network.domain.message.Message;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class NettyNetworkService implements NetworkService {

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("nikonov.torrentclient.logging.message");
    private final Map<PeerAddress, NettyChannel> channelMap;
    private final NioEventLoopGroup eventLoopGroup;
    private final EventService eventService;

    public NettyNetworkService(EventService eventService) {
        this.channelMap = new ConcurrentHashMap<>();
        this.eventLoopGroup = new NioEventLoopGroup();
        this.eventService = eventService;
    }

    @Override
    public <T extends Message> void send(T message) {
        var channel = channelMap.get(message.getRecipient());
        if (channel != null) {
            channel.write(message);
        }
    }

    @Override
    public void connect(PeerAddress address) {
        var channel = new NettyChannel(address, eventLoopGroup, eventService, resourceBundle);
        channel.connect();
        channelMap.put(address, channel);
    }

    @Override
    public void disconnect(PeerAddress address) {
        var channel = channelMap.get(address);
        if (channel != null) {
            channel.close();
            channelMap.remove(address);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {
        channelMap.values().forEach(NettyChannel::close);
        eventLoopGroup.shutdownGracefully();
    }
}
