package org.example.control.session;

import org.example.util.Session;

public class SessionUserContext implements UserContext {

    private final Session session;

    public SessionUserContext(Session session) {
        this.session = session;
    }

    @Override
    public Integer getCurrentUserId() {
        return session.getUserId();
    }

    @Override
    public String getCurrentUsername() {
        return session.getUser();
    }

    @Override
    public void setUsername(String username){
        session.setUser(username);
    }

    @Override
    public boolean isLoggedIn() {
        return session.getUserId() != null && session.getUser() != null;
    }

    @Override
    public void login(int userId, String username) {
        session.login(userId, username);
    }

    @Override
    public void setDemo(boolean demo) {
        session.setDemo(demo);
    }

    @Override
    public boolean isDemo() {
        return session.isDemo();
    }

    @Override
    public void logout(){
        session.logout();;
    }
}
