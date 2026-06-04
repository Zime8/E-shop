package org.example.config;

import javafx.util.Callback;
import org.example.control.LoginAppController;
import org.example.control.RegisterAppController;
import org.example.control.session.SessionUserContext;
import org.example.control.session.UserContext;
import org.example.dao.UserRepository;
import org.example.dao.db.DbUserDAO;
import org.example.util.Navigator;
import org.example.util.Session;

public class LoginContext {

    private final Session session;
    private final UserContext userContext;
    private final Navigator navigator;
    private final Callback<Class<?>, Object> controllerFactory;
    private final LoginAppController loginAppController;
    private final RegisterAppController registerAppController;

    public LoginContext() {
        this.session = new Session();
        this.userContext = new SessionUserContext(session);
        UserRepository userRepository = new DbUserDAO();
        this.loginAppController = new LoginAppController(userRepository, userContext);
        this.registerAppController = new RegisterAppController(userRepository);
        this.controllerFactory = new LoginFxControllerFactory(this);
        this.navigator = new Navigator(session, userContext, controllerFactory);
    }

    public Session getSession() {
        return session;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public Navigator getNavigator() {
        return navigator;
    }

    public Callback<Class<?>, Object> getControllerFactory() {
        return controllerFactory;
    }

    public LoginAppController getLoginAppController() {
        return loginAppController;
    }

    public RegisterAppController getRegisterAppController() {
        return registerAppController;
    }
}