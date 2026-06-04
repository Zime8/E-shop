package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.config.AppExecutors;
import org.example.config.LoginContext;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        LoginContext loginContext = new LoginContext();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        loader.setControllerFactory(loginContext.getControllerFactory());

        Parent root = loader.load();

        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        AppExecutors.shutdown();
    }
}