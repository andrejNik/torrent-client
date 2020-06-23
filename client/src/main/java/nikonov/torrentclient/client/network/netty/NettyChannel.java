package nikonov.torrentclient.client.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.event.EventService;
import nikonov.torrentclient.client.network.MessageCollector;
import nikonov.torrentclient.client.network.MessageDecoder;
import nikonov.torrentclient.client.network.MessageEncoder;
import nikonov.torrentclient.client.network.domain.Options;
import nikonov.torrentclient.client.network.domain.event.DisconnectEvent;
import nikonov.torrentclient.client.network.domain.message.Message;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.text.MessageFormat.format;

public class NettyChannel {

    private static final Logger logger = Logger.getLogger(NettyChannel.class.getName());
    private final PeerAddress peerAddress;
    private final EventService eventService;
    private final NioEventLoopGroup loopGroup;
    private final MessageEncoder messageEncoder;
    private final MessageDecoder messageDecoder;
    private final MessageCollector messageCollector;
    private final ResourceBundle resourceBundle;
    private Channel channel;

    public NettyChannel(PeerAddress peerAddress,
                        NioEventLoopGroup loopGroup,
                        EventService eventService,
                        ResourceBundle resourceBundle) {
        this.peerAddress = peerAddress;
        this.eventService = eventService;
        this.loopGroup = loopGroup;
        this.resourceBundle = resourceBundle;
        this.messageEncoder = new MessageEncoder();
        this.messageDecoder = new MessageDecoder();
        this.messageCollector = new MessageCollector();
    }

    public void connect() {
        var bootstrap = new Bootstrap();
        bootstrap
                .group(loopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)Options.CONNECTION_TIMEOUT.toMillis())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        var handler = new NettyChannelInboundHandler(peerAddress, eventService, messageDecoder, messageCollector, resourceBundle);
                        ch.pipeline().addLast(handler);
                    }
                });
        try {
            channel = bootstrap.connect(peerAddress.getIp(), peerAddress.getPort()).channel();
        } catch (Exception exp) {
            logger.log(
                    Level.SEVERE,
                    format(resourceBundle.getString("log.network.error.connect"), peerAddress.getIp(), peerAddress.getPort()),
                    exp
            );
            closeAndPublish();
        }
    }

    public <T extends Message> void write(T message) {
        try {
            var buff = Unpooled.wrappedBuffer(messageEncoder.encode(message));
            channel.writeAndFlush(buff);
        } catch (Exception exp) {
            logger.log(
                    Level.SEVERE,
                    format(resourceBundle.getString("log.network.error.write"), peerAddress.getIp(), peerAddress.getPort()),
                    exp
            );
            closeAndPublish();
        }
    }

    public void close() {
        try {
            channel.closeFuture();
        } catch (Exception exp) {
            logger.log(
                    Level.SEVERE,
                    format(resourceBundle.getString("log.network.error.close"), peerAddress.getIp(), peerAddress.getPort()),
                    exp
            );
        }
    }

    private void closeAndPublish() {
        eventService.publishEvent(new DisconnectEvent(peerAddress));
        try {
            channel.closeFuture();
        } catch (Exception exp) {
            logger.log(
                    Level.SEVERE,
                    format(resourceBundle.getString("log.network.error.close"), peerAddress.getIp(), peerAddress.getPort()),
                    exp
            );
        }
    }
}
