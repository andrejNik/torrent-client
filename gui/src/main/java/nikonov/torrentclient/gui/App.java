package nikonov.torrentclient.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    public static final String WINDOW_VIEW_LOCATION = "nikonov/torrentclient/gui/view/window-view.fxml";
    private AppController controller;

    @Override
    public void start(Stage stage) throws IOException {
        var fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource(WINDOW_VIEW_LOCATION));
        controller = new AppController(stage);
        fxmlLoader.setController(controller);
        var scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        try {
            controller.destroy();
        } finally {
            super.stop();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}