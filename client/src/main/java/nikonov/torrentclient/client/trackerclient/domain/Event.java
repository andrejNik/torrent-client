package nikonov.torrentclient.client.trackerclient.domain;

public enum Event {

    NONE(0),
    COMPLETED(1),
    STARTED(2),
    STOPPED(3);

    int code;

    Event(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}