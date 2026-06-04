package org.example.dao.demo;

import org.example.dao.UserRepository;
import org.example.demo.DemoData;
import org.example.models.dto.LoginResult;
import org.example.models.dto.LoginStatus;
import org.example.models.entity.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DemoUserDAO implements UserRepository {

    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";
    private static final Logger logger = Logger.getLogger(DemoUserDAO.class.getName());

    @Override
    public LoginResult checkLogin(String username, String password){

        requireNonBlank(username, USERNAME);
        requireNonBlank(password, PASSWORD);

        try {
            DemoData.ensureLoaded();
            DemoData.User u = DemoData.users().get(username);
            if (u == null) {
                return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);
            }

            boolean ok = BCrypt.checkpw(password, u.passHash());
            return ok
                    ? new LoginResult(LoginStatus.SUCCESS, u.role(), u.id())
                    : new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore login (demo)", e);
            return new LoginResult(LoginStatus.ERROR, null, null);
        }
    }

    @Override
    public boolean registerUser(String username, String password, String role, String email, String phone){

        requireNonBlank(username, USERNAME);
        requireNonBlank(password, PASSWORD);
        requireNonBlank(role, "Ruolo");

        try {
            DemoData.ensureLoaded();
            if (DemoData.users().containsKey(username)) return false;
            boolean emailTaken = DemoData.users().values().stream()
                    .anyMatch(u -> email != null && email.equalsIgnoreCase(u.email()));
            if (emailTaken) return false;

            int newId = DemoData.users().size() + 100;
            String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
            DemoData.users().put(username, new DemoData.User(newId, username, hashedPwd, role, email, phone));
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore registrazione (demo)", e);
            return false;
        }
    }

    @Override
    public boolean isUsernameTaken(String username){

        requireNonBlank(username, USERNAME);

        DemoData.ensureLoaded();
        return DemoData.users().containsKey(username);
    }

    @Override
    public boolean isEmailTaken(String email){

        requireNonBlank(email, "Email");

        DemoData.ensureLoaded();
        return DemoData.users().values().stream()
                .anyMatch(u -> u.email() != null && u.email().equalsIgnoreCase(email));
    }

    @Override
    public Integer findUserIdByUsername(String username){

        requireNonBlank(username, USERNAME);

        DemoData.ensureLoaded();
        DemoData.User u = DemoData.users().get(username);
        return u != null ? u.id() : null;
    }

    @Override
    public User findByUsername(String username){

        requireNonBlank(username, USERNAME);

        DemoData.ensureLoaded();
        DemoData.User du = DemoData.users().get(username);
        if (du == null) return null;
        return new User(
                du.username(),
                du.email(),
                du.phone(),
                du.passHash());
    }

    @Override
    public void updateProfile(String currentUsername, String newUsername, String email, String phone){

        requireNonBlank(currentUsername, "Username corrente");
        requireNonBlank(newUsername, "Nuovo username");

        try {
            DemoData.ensureLoaded();
            DemoData.User old = DemoData.users().get(currentUsername);
            if (old == null) return;
            renameDemoUserIfNeeded(currentUsername, newUsername);
            DemoData.users().put(newUsername, new DemoData.User(
                    old.id(), newUsername, old.passHash(), old.role(), email, phone
            ));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore aggiornamento profilo (demo)", e);
        }
    }

    @Override
    public void updateProfileWithPassword(String currentUsername, String newUsername,
                                   String email, String phone, String plainPassword){

        requireNonBlank(currentUsername, USERNAME);
        requireNonBlank(newUsername, "Nuovo username");
        requireNonBlank(plainPassword, PASSWORD);

        try {
            DemoData.ensureLoaded();
            DemoData.User old = DemoData.users().get(currentUsername);
            if (old == null) return;
            String hashedPwd = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            renameDemoUserIfNeeded(currentUsername, newUsername);
            DemoData.users().put(newUsername, new DemoData.User(
                    old.id(), newUsername, hashedPwd, old.role(), email, phone
            ));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore aggiornamento profilo con password (demo)", e);
        }
    }

    private void renameDemoUserIfNeeded(String currentUsername, String newUsername) {
        if (Objects.equals(currentUsername, newUsername)) {
            return;
        }

        if (DemoData.users().containsKey(newUsername)) {
            throw new IllegalStateException("Username già esistente (demo)");
        }

        DemoData.users().remove(currentUsername);
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " non valido");
        }
    }
}
