package nikonov.torrentclient.client.network;

import nikonov.torrentclient.client.domain.PeerAddress;
import nikonov.torrentclient.client.network.domain.message.Message;

public interface NetworkService {
    <T extends Message> void send(T message);
    void connect(PeerAddress address);
    void disconnect(PeerAddress address);
    void start();
    void close();
}
