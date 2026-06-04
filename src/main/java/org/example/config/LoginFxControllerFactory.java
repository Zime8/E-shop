package org.example.config;

import javafx.util.Callback;
import org.example.boundary.LoginController;
import org.example.boundary.RegisterController;

import java.lang.reflect.InvocationTargetException;

public class LoginFxControllerFactory implements Callback<Class<?>, Object> {

    private final LoginContext loginContext;

    public LoginFxControllerFactory(LoginContext loginContext) {
        this.loginContext = loginContext;
    }

    @Override
    public Object call(Class<?> type) {
        if (type == LoginController.class) {
            LoginController controller = new LoginController();
            controller.setAppController(loginContext.getLoginAppController());
            controller.setNavigator(loginContext.getNavigator());
            return controller;
        }

        if (type == RegisterController.class) {
            RegisterController controller = new RegisterController();
            controller.setAppController(loginContext.getRegisterAppController());
            controller.setNavigator(loginContext.getNavigator());
            return controller;
        }

        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new ControllerCreationException(
                    "Impossibile creare il controller: " + type.getName(), e);
        }
    }
}