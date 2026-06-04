package org.example.dao;

import org.example.models.dto.LoginResult;
import org.example.models.entity.User;

public interface UserRepository {
    LoginResult checkLogin(String username, String password);
    boolean registerUser(String username, String password, String role, String email, String phone);
    boolean isUsernameTaken(String username);
    boolean isEmailTaken(String email);
    Integer findUserIdByUsername(String username);
    User findByUsername(String username);
    void updateProfile(String currentUsername, String newUsername, String email, String phone);
    void updateProfileWithPassword(String currentUsername, String newUsername,
                                   String email, String phone, String plainPassword);
}
