package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.controllers.app.LoginAppController;
import org.example.controllers.ui.LoginController;


public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();

        LoginController loginUI = loader.getController();
        loginUI.setAppController(new LoginAppController());
        stage.setScene(new Scene(root));
        stage.setTitle("Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
