package org.example.cli;

public final class CliSession {
    private static volatile boolean authenticated = false;
    private static volatile Integer userId = null;
    private static volatile String username = null;
    private static volatile String role = null;

    private CliSession() {}

    public static boolean isAuthenticated() { return authenticated; }
    public static Integer userId() { return userId; }
    public static String username() { return username; }
    public static String role() { return role; }

    static void setAuthenticated(String user, Integer id, String r) {
        authenticated = true;
        username = user;
        userId = id;
        role = r;
    }

    public static void clear() {
        authenticated = false;
        username = null;
        userId = null;
        role = null;
    }
}
