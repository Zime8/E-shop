package org.example.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Navigator {

    private static final Logger logger = Logger.getLogger(Navigator.class.getName());

    private Navigator() {
        throw new UnsupportedOperationException("Utility class - non serve istanziare");
    }

    public static void openModal(String fxmlPath, Object data, Runnable onCloseCallback) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Object uiController = loader.getController();

            injectAppController(uiController);

            if (data != null) {
                tryInvoke(uiController, "loadData", data);
            }

            Stage dialog = createModalStage(root);
            tryInvoke(uiController, "setStage", dialog);

            dialog.showAndWait();
            if(onCloseCallback != null) onCloseCallback.run();

            refreshCaller();

        } catch (IOException e) {
            throw new IllegalArgumentException("Errore apertura " + fxmlPath, e);
        }
    }

    private static void refreshCaller() {
        Window owner = Window.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing())
                .findFirst().orElse(null);
        if (owner != null && owner.getScene().getRoot().getUserData() instanceof Runnable refresher) {
            Platform.runLater(refresher);
        }
    }


    private static void injectAppController(Object uiController) {
        try {
            String appSimpleName = uiController.getClass().getSimpleName().replace("Controller", "AppController");
            String appClassName = "org.example.controllers.app." + appSimpleName;

            Class<?> appClass = Class.forName(appClassName);
            Object appController = appClass.getDeclaredConstructor().newInstance();

            Method setter = findSetter(uiController.getClass());
            if (setter != null) {
                setter.invoke(uiController, appController);
            } else {
                logger.log(Level.SEVERE, "Inject AppController failed: {0}", uiController.getClass().getName());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Inject AppController FAILED", e);
        }
    }

    private static Method findSetter(Class<?> clazz) {
        try {
            return clazz.getMethod("setAppController", Object.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static void tryInvoke(Object target, String methodName, Object... args) {
        // Prova prima single Object
        if (args.length == 1) {
            try {
                Method method = target.getClass().getMethod(methodName, Object.class);
                method.invoke(target, args[0]);
                return;
            } catch (Exception e) {
                // Ignora
            }
        }

        // Poi multi-arg
        Class<?>[] paramTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class[]::new);

        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            method.invoke(target, args);
        } catch (NoSuchMethodException e) {
            logger.fine(() -> String.format("No method %s(%s)",
                    methodName, Arrays.toString(paramTypes)));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Invoke FAILED: {0}", e);
        }
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
}