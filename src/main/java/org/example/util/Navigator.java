package org.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Callback;
import org.example.config.AppContext;
import org.example.control.session.UserContext;
import org.example.models.dto.LoginResult;
import org.example.models.dto.LoginStatus;

import java.io.IOException;
import java.util.function.Consumer;

public class Navigator {

    private AppContext appContext;
    private final Session session;
    private final UserContext userContext;
    private final Callback<Class<?>, Object> loginControllerFactory;

    public Navigator(Session session, UserContext userContext, Callback<Class<?>, Object> loginControllerFactory) {
        this.session = session;
        this.userContext = userContext;
        this.loginControllerFactory = loginControllerFactory;
    }

    public void goToBuyerHome(Stage stage) {
        try {
            FXMLLoader loader = createAppLoader("/fxml/Home.fxml");
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setTitle("Home");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Errore caricamento Home.fxml", e);
        }
    }

    public void goToSellerHome(Stage stage) {
        try {
            FXMLLoader loader = createAppLoader("/fxml/SellerHome.fxml");
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setTitle("Area Venditore");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Errore caricamento SellerHome.fxml", e);
        }
    }

    public void goToLogin(Stage stage) {
        try {
            appContext = null;
            FXMLLoader loader = createLoginLoader("/fxml/Login.fxml");
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Errore caricamento Login.fxml", e);
        }
    }

    public void goAfterLogin(Stage stage, LoginResult result) {
        if (result == null || result.status() != LoginStatus.SUCCESS) {
            return;
        }

        appContext = new AppContext(session, userContext, this);

        if ("venditore".equalsIgnoreCase(result.role())) {
            goToSellerHome(stage);
        } else {
            goToBuyerHome(stage);
        }
    }

    public void goToRegister(Stage stage) {
        try {
            FXMLLoader loader = createLoginLoader("/fxml/Register.fxml");
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.setTitle("Registrazione");
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Errore caricamento Register.fxml", e);
        }
    }

    public <T> void openModal(String fxmlPath, Consumer<T> controllerInitializer) {
        try {
            FXMLLoader loader = createAppLoader(fxmlPath);
            Parent root = loader.load();

            T controller = loader.getController();

            if (controllerInitializer != null) {
                controllerInitializer.accept(controller);
            }

            Stage dialog = createModalStage(root);
            dialog.showAndWait();

        } catch (IOException e) {
            throw new RuntimeException("Errore apertura finestra: " + fxmlPath, e);
        }
    }

    public <T> void openTransparentModal(String fxmlPath, Consumer<T> controllerInitializer) {
        try {
            FXMLLoader loader = createAppLoader(fxmlPath);
            Parent root = loader.load();

            T controller = loader.getController();
            if (controllerInitializer != null) {
                controllerInitializer.accept(controller);
            }

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);

            stage.initModality(Modality.APPLICATION_MODAL);

            Window owner = Window.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);

            if (owner != null) {
                stage.initOwner(owner);
                stage.setX(owner.getX());
                stage.setY(owner.getY());
                stage.setWidth(owner.getWidth());
                stage.setHeight(owner.getHeight());
            }

            stage.showAndWait();

        } catch (IOException e) {
            throw new RuntimeException("Errore apertura finestra trasparente: " + fxmlPath, e);
        }
    }

    private FXMLLoader createLoginLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(loginControllerFactory);
        return loader;
    }

    public FXMLLoader createAppLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(getOrCreateAppContext().getControllerFactory());
        return loader;
    }

    private static Stage createModalStage(Parent root) {
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        Window.getWindows().stream().filter(Window::isShowing).findFirst().ifPresent(stage::initOwner);
        stage.setResizable(false);
        stage.centerOnScreen();
        return stage;
    }

    private AppContext getOrCreateAppContext() {
        if (appContext == null) {
            appContext = new AppContext(session, userContext, this);
        }
        return appContext;
    }
}