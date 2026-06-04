package org.example.control.session;

public interface UserContext {
    Integer getCurrentUserId();
    String getCurrentUsername();
    void setUsername(String username);
    boolean isLoggedIn();
    boolean isDemo();
    void login(int userId, String username);
    void setDemo(boolean demo);
    void logout();
}
