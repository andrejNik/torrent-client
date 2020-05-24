package nikonov.torrentclient.network.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.event.EventService;
import nikonov.torrentclient.network.MessageCollector;
import nikonov.torrentclient.network.MessageDecoder;
import nikonov.torrentclient.network.domain.event.ConnectToPeerEvent;
import nikonov.torrentclient.network.domain.event.DisconnectEvent;
import nikonov.torrentclient.network.domain.event.MessageEvent;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.text.MessageFormat.format;

public class NettyChannelInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(NettyChannelInboundHandler.class.getName());
    public final PeerAddress peerAddress;
    public final EventService eventService;
    private final MessageDecoder messageDecoder;
    private final MessageCollector messageCollector;
    private final ResourceBundle resourceBundle;

    public NettyChannelInboundHandler(PeerAddress peerAddress,
                                      EventService eventService,
                                      MessageDecoder messageDecoder,
                                      MessageCollector messageCollector,
                                      ResourceBundle resourceBundle) {
        this.peerAddress = peerAddress;
        this.eventService = eventService;
        this.messageDecoder = messageDecoder;
        this.messageCollector = messageCollector;
        this.resourceBundle = resourceBundle;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        eventService.publishEvent(new ConnectToPeerEvent(peerAddress));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeAndPublish(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        var buff = (ByteBuf) msg;
        try {
            var byteArray = new byte[buff.readableBytes()];
            buff.readBytes(byteArray);
            messageCollector.addBytes(byteArray);
            for (var array : messageCollector.messages()) {
                var message = messageDecoder.decode(array);
                message.setSender(peerAddress);
                eventService.publishEvent(new MessageEvent(peerAddress, message));
            }
        } catch (Exception exp) {
            logger.log(
                    Level.SEVERE,
                    format(resourceBundle.getString("log.network.error.read"), peerAddress.getIp(), peerAddress.getPort()),
                    exp
            );
            closeAndPublish(ctx);
        } finally {
            buff.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(
                Level.SEVERE,
                format(resourceBundle.getString("log.network.error"), peerAddress.getIp(), peerAddress.getPort()),
                cause
        );
        closeAndPublish(ctx);
    }

    private void closeAndPublish(ChannelHandlerContext ctx) {
        eventService.publishEvent(new DisconnectEvent(peerAddress));
        ctx.close();
    }
}
