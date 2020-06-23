package nikonov.torrentclient.client.trackerclient;

import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.trackerclient.domain.*;
import nikonov.torrentclient.client.trackerclient.domain.udp.PackageType;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;

/**
 * TODO tracker id в ответ ?
 */
public class UDPTrackerClient extends TrackerClient {

    public final long PROTOCOL_ID = 0x41727101980L;
    public final int ANNOUNCE_RESPONSE_PACKET_SIZE = 20 + 6 * 50;
    public final int CONNECT_RESPONSE_PACKET_SIZE = 16;

    @Override
    public TrackerResponse request(TrackerRequest request) {
        try {
            var transactionId = new Random().nextInt();
            var connectionId = sendRequestAndGetAnswerOrNull(
                    connectPacket(transactionId, request),
                    packet -> connectionIdFromPacket(packet, transactionId), CONNECT_RESPONSE_PACKET_SIZE);
            if (connectionId != null) {
                return sendRequestAndGetAnswerOrNull(
                        announcePacket(connectionId, transactionId, request),
                        packet -> trackerResponseFromPacket(packet, transactionId), ANNOUNCE_RESPONSE_PACKET_SIZE);
            }
        } catch (Throwable ignore) {
            // TODO логирование
        }
        return null;
    }

    /**
     * Получить TrackerResponse из пакета или null
     */
    private TrackerResponse trackerResponseFromPacket(DatagramPacket packet, int transactionId) {
        if (packet.getData().length > 16) {
            var buffer = ByteBuffer.wrap(packet.getData());
            var action = buffer.getInt();
            var receivedTransactionId = buffer.getInt();
            if (action == PackageType.ANNOUNCE.getCode() && receivedTransactionId == transactionId) {
                var interval = buffer.getInt();
                var leechers = buffer.getInt();
                var seeders = buffer.getInt();
                var peerAddresses = new ArrayList<PeerAddress>();
                var countPeers = (packet.getData().length - 20) / 6;
                for(var i = 0; i < countPeers; i++) {
                    peerAddresses.add(peerAddress(buffer));
                }
                return new TrackerResponse(interval, leechers, seeders, null, peerAddresses);
            }
        }
        return null;
    }

    /**
     * Получить connectionId из пакета или null
     */
    private Long connectionIdFromPacket(DatagramPacket packet, int transactionId) {
        if (packet.getData().length == 16) {
            var buffer = ByteBuffer.wrap(packet.getData());
            var actionCode = buffer.getInt();
            var receivedTransactionId = buffer.getInt();
            var connectionId = buffer.getLong();
            if (transactionId == receivedTransactionId && actionCode == PackageType.CONNECT.getCode()) {
                return connectionId;
            }
        }
        return null;
    }

    /**
     * Сформировать connect-пакет
     */
    private DatagramPacket connectPacket(int transactionId, TrackerRequest request) {
        try {
            var array = ByteBuffer
                    .allocate(16)
                    .putLong(PROTOCOL_ID)
                    .putInt(PackageType.CONNECT.getCode())
                    .putInt(transactionId)
                    .array();
            return new DatagramPacket(array, array.length, InetAddress.getByName(request.getTrackerInfo().getHost()), request.getTrackerInfo().getPort());
        } catch (Throwable exp) {
            throw new RuntimeException(exp);
        }
    }

    /**
     * Сформировать announce-пакет
     */
    private DatagramPacket announcePacket(long connectionId, int transactionId, TrackerRequest request) {
        try {
            var array = ByteBuffer
                    .allocate(98)
                    .putLong(connectionId)
                    .putInt(PackageType.ANNOUNCE.getCode())
                    .putInt(transactionId)
                    .put(request.getInfoHash())
                    .put(request.getClientInfo().getPeerId())
                    .putLong(request.getDownloaded())
                    .putLong(request.getLeft())
                    .putLong(request.getUploaded())
                    .putInt(request.getEvent().getCode())
                    .putInt(0)  // FIXME Дефолтный ip для UPD протокола трекера
                    .putInt(0)  // FIXME Key - рандомно выбрал ноль тк не знаю что писать
                    .putInt(-1) // FIXME Дефолтный numwant для UDP протокола
                    .putShort((short) request.getClientInfo().getPort())
                    .array();
            return new DatagramPacket(array, array.length, InetAddress.getByName(request.getTrackerInfo().getHost()), request.getTrackerInfo().getPort());
        } catch (Throwable exp) {
            throw new RuntimeException(exp);
        }
    }

    /**
     * Отравить запрос и вернуть преобразованный ответ или null
     *
     * @param sendPacket             пакет для отправки
     * @param packetToResultFunction функция преобразования ответа
     * @param receivePackageSize     ожидаемый размер пакета с ответом
     */
    private <T> T sendRequestAndGetAnswerOrNull(DatagramPacket sendPacket, Function<DatagramPacket, T> packetToResultFunction, int receivePackageSize) {
        try (var socket = new DatagramSocket()) {
            for (var n = 1; n < 3; n++) {
                socket.setSoTimeout(15 * (int) Math.pow(2, n));
                socket.send(sendPacket);
                var receivePackage = new DatagramPacket(new byte[receivePackageSize], receivePackageSize);
                try {
                    socket.receive(receivePackage);
                    return packetToResultFunction.apply(receivePackage);
                } catch (SocketTimeoutException ignore) {

                }
            }
        } catch (Throwable ignore) {
            // TODO логирование
        }
        return null;
    }
}
