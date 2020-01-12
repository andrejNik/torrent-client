package nikonov.torrentclient.download;

import nikonov.torrentclient.domain.PeerAddress;

import java.util.Set;

public interface PeerAddressSupplierService {

    Set<PeerAddress> get();
    void pieceDownload(int pieceLength);
    void pieceUpload(int pieceLength);
    void complete();
}
