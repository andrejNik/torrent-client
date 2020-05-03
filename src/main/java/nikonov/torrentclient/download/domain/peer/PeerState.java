package nikonov.torrentclient.download.domain.peer;

/**
 * состояние пира
 */
public enum PeerState {
    /**
     * пир подключился, но не прислал handshake и unchoke сообщение
     */
    CONNECT,
    /**
     * пир прислал handshake и unchoke сообщение
     */
    ACTIVE;
}
