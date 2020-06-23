package nikonov.torrentclient.client.trackerclient;

import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.trackerclient.domain.TrackerRequest;
import nikonov.torrentclient.client.trackerclient.domain.TrackerResponse;

import java.nio.ByteBuffer;

public abstract class TrackerClient {
    /**
     * Запрос списка участников раздачи
     */
    public abstract TrackerResponse request(TrackerRequest request);

    protected PeerAddress peerAddress(ByteBuffer buffer) {
        var ip = buffer.getInt();
        // FIXME buffer.getShort() возвращает отрицательные числа. возможно потому что в ответах от трекера используется беззнаковые числа
        // FIXME правильно ли я тогда получаю ip ?
        var port = ByteBuffer.wrap(new byte[] {0x00, 0x00, buffer.get(), buffer.get()}).getInt();
        return new PeerAddress(intToIpString(ip), port);
    }

    protected String intToIpString(int ip) {
        return String.format("%d.%d.%d.%d", (ip >> 24 & 0xff), (ip >> 16 & 0xff), (ip >> 8 & 0xff), (ip & 0xff));
    }
}
