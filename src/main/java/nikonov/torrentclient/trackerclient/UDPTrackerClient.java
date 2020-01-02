package nikonov.torrentclient.trackerclient;

import nikonov.torrentclient.metadata.domain.metadata.TrackerProtocol;
import nikonov.torrentclient.trackerclient.domain.*;
import nikonov.torrentclient.trackerclient.domain.udp.PackageType;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;


public class UDPTrackerClient implements TrackerClient {

    private final long PROTOCOL_ID = 0x41727101980L;
    private final int ANNOUNCE_RESPONSE_PACKET_SIZE = 20 + 6 * 50;
    private final int CONNECT_RESPONSE_PACKET_SIZE = 16;

    @Override
    public boolean isSupport(TrackerProtocol protocol) {
        return protocol == TrackerProtocol.UDP;
    }

    @Override
    public TrackerResponse request(TrackerRequest request) {
        try {
            var transactionId = new Random().nextInt();
            var connectionId = request(
                    connectPacket(transactionId, request),
                    packet -> packetConnectionId(packet, transactionId), CONNECT_RESPONSE_PACKET_SIZE);
            if (connectionId != null) {
                return request(
                        announcePacket(connectionId, transactionId, request),
                        packet -> packetTrackerResponse(packet, transactionId), ANNOUNCE_RESPONSE_PACKET_SIZE);
            }
        } catch (Throwable exp) {
            throw new RuntimeException(exp);
        }
        return null;
    }

    /**
     * Получить TrackerResponse из пакета или null
     */
    private TrackerResponse packetTrackerResponse(DatagramPacket packet, int transactionId) {
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
                    var ip = buffer.getInt();
                    // FIXME buffer.getShort() возвращает отрицательные числа. возможно потому что в ответах от трекера используется беззнаковые числа
                    // FIXME правильно ли я тогда получаю ip ?
                    var port = ByteBuffer.wrap(new byte[] {0x00, 0x00, buffer.get(), buffer.get()}).getInt();
                    peerAddresses.add(new PeerAddress(intToIpString(ip), port));
                }
                return new TrackerResponse(interval, leechers, seeders, peerAddresses);
            }
        }
        return null;
    }

    /**
     * Получить connectionId из пакета или null
     */
    private Long packetConnectionId(DatagramPacket packet, int transactionId) {
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
    private <T> T request(DatagramPacket sendPacket, Function<DatagramPacket, T> packetToResultFunction, int receivePackageSize) {
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
            // TODO Логировать ?
        }
        return null;
    }

    private String intToIpString(int ip) {
        return String.format("%d.%d.%d.%d", (ip >> 24 & 0xff), (ip >> 16 & 0xff), (ip >> 8 & 0xff), (ip & 0xff));
    }
}
