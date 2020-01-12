package nikonov.torrentclient.network;

import nikonov.torrentclient.domain.PeerAddress;
import nikonov.torrentclient.network.domain.message.Message;

public interface NetworkService {
    <T extends Message> void send(T message);
    void connect(PeerAddress address);
    void start();
    void close();
}
