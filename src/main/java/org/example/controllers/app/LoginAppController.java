package org.example.controllers.app;

import org.example.dao.UserDAO;
import org.example.demo.DemoData;
import org.example.util.Session;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginAppController {
    private static final Logger logger = Logger.getLogger(LoginAppController.class.getName());

    private final UserDAO userDAO = UserDAO.getInstance();

    public LoginResult performLogin(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return new LoginResult(LoginStatus.INVALID_INPUT, null, null);
        }

        UserDAO.LoginResult daoResult = userDAO.checkLogin(username.trim(), password);
        return switch (daoResult.status()) {
            case SUCCESS -> {
                Session.setUser(username.trim());
                Session.setUserId(daoResult.userId());
                yield new LoginResult(LoginStatus.SUCCESS, daoResult.role(), null);
            }
            case INVALID_CREDENTIALS -> new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);
            case ERROR -> new LoginResult(LoginStatus.ERROR, null, null);
        };
    }

    public void performDemoLogin() {
        try {
            String guest = "ospite-" + UUID.randomUUID().toString().substring(0, 8);
            int demoId = DemoData.NEXT_DEMO_USER_ID.getAndDecrement();

            Session.clear();
            Session.setDemo(true);
            Session.setUser(guest);
            Session.setUserId(demoId);

            DemoData.ensureLoaded();
            DemoData.users().putIfAbsent(guest, new DemoData.User(demoId, guest, null, "utente", null, null));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore modalità demo", e);
            throw new RuntimeException("Impossibile demo", e);
        }
    }

    public String getNextView(LoginResult result) {
        if (result.status() == LoginStatus.SUCCESS && "venditore".equalsIgnoreCase(result.role())) {
            return "/fxml/SellerHome.fxml";
        } else if (result.status() == LoginStatus.SUCCESS) {
            return "/fxml/Home.fxml";
        }
        return null;
    }

    public static class LoginResult {
        private final LoginStatus status;
        private final String role;
        private final Integer userId;

        public LoginResult(LoginStatus status, String role, Integer userId) {
            this.status = status;
            this.role = role;
            this.userId = userId;
        }

        public LoginStatus status() { return status; }
        public String role() { return role; }
        public Integer userId() { return userId; }
    }

    public enum LoginStatus {
        SUCCESS, INVALID_INPUT, INVALID_CREDENTIALS, ERROR
    }
}

