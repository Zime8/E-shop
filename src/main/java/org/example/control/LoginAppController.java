package org.example.control;

import org.example.control.session.UserContext;
import org.example.dao.UserRepository;
import org.example.demo.DemoData;
import org.example.models.dto.LoginResult;
import org.example.models.dto.LoginStatus;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginAppController {
    private static final Logger logger = Logger.getLogger(LoginAppController.class.getName());

    private final UserRepository userRepository;
    private final UserContext userContext;

    public LoginAppController(UserRepository userRepository, UserContext userContext) {
        this.userRepository = userRepository;
        this.userContext = userContext;
    }

    public LoginResult performLogin(String username, String password) {

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return new LoginResult(LoginStatus.INVALID_INPUT, null, null);
        }

        String normalizedUsername = username.trim();
        LoginResult daoResult = userRepository.checkLogin(normalizedUsername, password);

        if (daoResult.status() == LoginStatus.SUCCESS) {
            userContext.login(daoResult.userId(), normalizedUsername);
            userContext.setDemo(false);
        }

        return switch (daoResult.status()) {
            case SUCCESS -> new LoginResult(LoginStatus.SUCCESS, daoResult.role(), daoResult.userId());
            case INVALID_CREDENTIALS -> new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);
            case ERROR -> new LoginResult(LoginStatus.ERROR, null, null);
            default -> new LoginResult(LoginStatus.INVALID_INPUT, null, null);
        };
    }

    public void performDemoLogin() {
        try {
            String guest = "ospite-" + UUID.randomUUID().toString().substring(0, 8);
            int demoId = DemoData.NEXT_DEMO_USER_ID.getAndDecrement();

            userContext.login(demoId, guest);
            userContext.setDemo(true);

            DemoData.ensureLoaded();
            DemoData.users().putIfAbsent(guest, new DemoData.User(demoId, guest, null, "utente", null, null));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore modalità demo:", e);
        }
    }

}

