package nikonov.torrentclient.trackerclient.domain.udp;

public enum PackageType {

    CONNECT(0),
    ANNOUNCE(1);

    int code;

    PackageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
