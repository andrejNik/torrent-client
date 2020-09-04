package nikonov.torrentclient.client.trackerclient;

import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.base.metadata.BencodeReader;
import nikonov.torrentclient.base.metadata.domain.BencodeItem;
import nikonov.torrentclient.client.trackerclient.domain.TrackerRequest;
import nikonov.torrentclient.client.trackerclient.domain.TrackerResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;

public class HTTPTrackerClient extends TrackerClient {

    public static final String MIN_INTERVAL_KEY = "min interval";
    public static final String INTERVAL_KEY = "interval";
    public static final String TRACKER_ID_KEY = "tracker id";
    public static final String COMPLETE_KEY = "complete";
    public static final String INCOMPLETE_KEY = "incomplete";
    public static final String PEERS_KEY = "peers";
    public static final String IP_KEY = "ip";
    public static final String PORT_KEY = "port";

    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Override
    public TrackerResponse request(TrackerRequest request) {
        var body = httpRequest(request);
        if (body != null && new String(body).contains(PEERS_KEY)) { // contains(body) - проверка наличия пиров в ответе. может придти строка <title>Invalid request </title>
            var content = new BencodeReader(body).content();
            if (!content.isEmpty()) {
                var trackerResponse = new TrackerResponse();
                var map = (Map<String, BencodeItem>) content.get(0).getValue();
                if (map.containsKey(MIN_INTERVAL_KEY) || map.containsKey(INTERVAL_KEY)) {
                    var interval = map.containsKey(MIN_INTERVAL_KEY) ?
                            (Long) map.get(MIN_INTERVAL_KEY).getValue() :
                            (Long) map.get(INTERVAL_KEY).getValue();
                    trackerResponse.setInterval(interval.intValue());
                }
                if (map.containsKey(INCOMPLETE_KEY)) {
                    var leechers = (Long)map.get(INCOMPLETE_KEY).getValue();
                    trackerResponse.setLeechers(leechers.intValue());
                }
                if (map.containsKey(COMPLETE_KEY)) {
                    var seeders = (Long)map.get(COMPLETE_KEY).getValue();
                    trackerResponse.setSeeders(seeders.intValue());
                }
                if (map.containsKey(TRACKER_ID_KEY)) {
                    var trackerId = (String)map.get(TRACKER_ID_KEY).getValue();
                    trackerResponse.setTrackerId(trackerId);
                }
                trackerResponse.setPeerAddresses(peerAddressList(map.get(PEERS_KEY), body));
                return trackerResponse;
            }
        }
        return null;
    }

    private List<PeerAddress> peerAddressList(BencodeItem peers, byte[] body) {
        var addressList = new ArrayList<PeerAddress>();
        if (peers.getValue() instanceof String) {
            var buffer = ByteBuffer.wrap(Arrays.copyOfRange(body, peers.getIndexStart(), peers.getIndexEnd() + 1));
            while(buffer.hasRemaining()) {
                addressList.add(peerAddress(buffer));
            }
        } else {
            var peersList = (List<BencodeItem>)peers.getValue();
            for(var bencodeItem : peersList) {
                var peerAddressMap = (Map<String, BencodeItem>) bencodeItem.getValue();
                var ip = (String)peerAddressMap.get(IP_KEY).getValue();
                var port = (Long)peerAddressMap.get(PORT_KEY).getValue();
                addressList.add(new PeerAddress(ip, port.intValue()));
            }
        }
        return addressList;
    }

    private byte[] httpRequest(TrackerRequest request) {
        try {
            var host = request.getTrackerInfo().getHost();
            var port = request.getTrackerInfo().getPort() != null ? request.getTrackerInfo().getPort() : 80;
            var additional = request.getTrackerInfo().getAdditional() != null ? request.getTrackerInfo().getAdditional() : "";
            var uriString = format(
                    "http://{0}:{1}{2}{3}info_hash={4}&peer_id={5}&port={6}&uploaded={7}&downloaded={8}&left={9}&event={10}",
                    host,                                              // 0
                    String.valueOf(port),                              // 1
                    additional,                                        // 2
                    additional.contains("?") ? "&" : "?",              // 3
                    encode(request.getInfoHash()),                     // 4
                    encode(request.getClientInfo().getPeerId()),       // 5
                    String.valueOf(request.getClientInfo().getPort()), // 6
                    String.valueOf(request.getUploaded()),             // 7
                    String.valueOf(request.getDownloaded()),           // 8
                    String.valueOf(request.getLeft()),                 // 9
                    request.getEvent().toString().toLowerCase()       // 10
            );
            var httpRequest = HttpRequest
                    .newBuilder()
                    .uri(new URI(uriString))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            var httpResponse = HttpClient
                    .newBuilder()
                    .build()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            return httpResponse.statusCode() == 200 ? httpResponse.body() : null;
        } catch (URISyntaxException | IOException | InterruptedException ignore) {
            // TODO логирование
        }
        return null;
    }

    private String encode(byte[] in) {
        byte ch = 0x00;
        var i = 0;
        if (in == null || in.length <= 0)
            return null;

        var pseudo = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        var out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            // First check to see if we need ASCII or HEX
            if ((in[i] >= '0' && in[i] <= '9')
                    || (in[i] >= 'a' && in[i] <= 'z')
                    || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$'
                    || in[i] == '-' || in[i] == '_' || in[i] == '.'
                    || in[i] == '!') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0); // Strip off high nibble
                ch = (byte) (ch >>> 4); // shift the bits down
                ch = (byte) (ch & 0x0F); // must do this is high order bit is
                // on!
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                ch = (byte) (in[i] & 0x0F); // Strip off low nibble
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                i++;
            }
        }
        return new String(out);
    }
}
