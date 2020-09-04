module nikonov {
    requires javafx.controls;
    requires javafx.fxml;
    requires uk.co.caprica.vlcj.javafx;
    requires uk.co.caprica.vlcj;
    requires lombok;
    requires base;

    opens nikonov.torrentclient.gui to javafx.fxml;
    exports nikonov.torrentclient.gui;
}