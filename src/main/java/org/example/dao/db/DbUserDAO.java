package org.example.dao.db;

import org.example.dao.UserRepository;
import org.example.database.DatabaseConnection;
import org.example.models.entity.User;
import org.example.models.dto.LoginStatus;
import org.example.models.dto.LoginResult;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DbUserDAO implements UserRepository {

    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";
    private static final Logger logger = Logger.getLogger(DbUserDAO.class.getName());

    // AUTH / PROFILO
    @Override
    public LoginResult checkLogin(String username, String password) {

        requireNonBlank(username, USERNAME);
        requireNonBlank(password, PASSWORD);

        final String call = "{ call sp_user_login(?) }";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                if (!rs.next())
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                int userId = rs.getInt("id_user");
                String passFromDb = rs.getString("pass");
                String role = rs.getString("rol");

                if (passFromDb == null)
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                boolean ok = passFromDb.startsWith("$2")
                        ? BCrypt.checkpw(password, passFromDb)
                        : password.equals(passFromDb);

                if (!ok)
                    return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, null);

                upgradePasswordHashIfNeeded(conn, userId, password, passFromDb);
                return new LoginResult(LoginStatus.SUCCESS, role, userId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il check login", e);
            return new LoginResult(LoginStatus.ERROR, null, null);
        }
    }

    private boolean isBCryptHash(String value) {
        return value != null && value.startsWith("$2");
    }

    private void upgradePasswordHashIfNeeded(Connection conn, int userId, String rawPassword, String storedPassword) throws SQLException {
        if (!isBCryptHash(storedPassword)) {
            String newHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
            try (PreparedStatement up = conn.prepareStatement(
                    "UPDATE users SET pass = ? WHERE id_user = ?")) {
                up.setString(1, newHash);
                up.setInt(2, userId);
                up.executeUpdate();
            }
        }
    }

    @Override
    public boolean registerUser(String username, String password, String role, String email, String phone) {

        requireNonBlank(username, USERNAME);
        requireNonBlank(password, PASSWORD);
        requireNonBlank(role, "Ruolo");

        final String call = "{ call sp_user_register(?, ?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            String hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt(12));
            cs.setString(1, username);
            cs.setString(2, hashedPwd);
            cs.setString(3, role);
            cs.setString(4, email);
            cs.setString(5, phone);
            cs.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante la registrazione utente", e);
            return false;
        }
    }

    @Override
    public boolean isUsernameTaken(String username) {

        requireNonBlank(username, USERNAME);

        final String call = "{ call sp_user_check_username(?) }";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo username esistente", e);
            return false;
        }
    }

    @Override
    public boolean isEmailTaken(String email) {

        requireNonBlank(email, "Email");

        final String call = "{ call sp_user_check_email(?) }";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setString(1, email);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore controllo email esistente", e);
            return false;
        }
    }

    @Override
    public Integer findUserIdByUsername(String username) {

        requireNonBlank(username, USERNAME);

        try {
            final String call = "{ call sp_user_find_id(?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, username);
                try (ResultSet rs = cs.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : null;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante la findUserIdByUsername", e);
            return null;
        }
    }

    @Override
    public User findByUsername(String username) {

        requireNonBlank(username, USERNAME);

        try {
            final String call = "{ call sp_user_find_by_username(?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, username);
                try (ResultSet rs = cs.executeQuery()) {
                    if (!rs.next()) return null;
                    return new User(
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("pass")
                    );
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Errore durante findByUsername", e);
            return null;
        }
    }

    @Override
    public void updateProfile(String currentUsername, String newUsername, String email, String phone) {

        requireNonBlank(currentUsername, USERNAME);
        requireNonBlank(newUsername, "Nuovo username");

        try {
            final String call = "{ call sp_user_update_profile(?, ?, ?, ?) }";
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, currentUsername);
                cs.setString(2, newUsername);
                cs.setString(3, email);
                cs.setString(4, phone);
                cs.executeUpdate();
            }
        } catch (SQLException e){
            logger.log(Level.WARNING, "Errore durante la updateProfile", e);
        }
    }

    @Override
    public void updateProfileWithPassword(String currentUsername, String newUsername,
                                                 String email, String phone, String plainPassword) {

        requireNonBlank(currentUsername, USERNAME);
        requireNonBlank(newUsername, "Nuovo username");
        requireNonBlank(plainPassword, PASSWORD);

        try {
            final String call = "{ call sp_user_update_profile_pwd(?, ?, ?, ?, ?) }";
            String hashedPwd = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            try (Connection conn = DatabaseConnection.getConnection();
                 CallableStatement cs = conn.prepareCall(call)) {
                cs.setString(1, currentUsername);
                cs.setString(2, newUsername);
                cs.setString(3, email);
                cs.setString(4, phone);
                cs.setString(5, hashedPwd);
                cs.executeUpdate();
            }
        } catch (SQLException e){
            logger.log(Level.WARNING, "Errore durante la updateProfileWithPassword", e);
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " non valido");
        }
    }
}
