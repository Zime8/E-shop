package org.example.cli;

import org.example.dao.UserDAO;
import org.example.models.LoginResult;
import org.example.models.LoginStatus;
import org.example.util.Session;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("java:S106")
final class AuthCli {

    private static final UserDAO dao =  new UserDAO();

    private AuthCli() {}

    static void handleLoginCommand(String[] args) {
        Map<String, String> params = parseArgs(args);
        String user = params.get("user");
        String pass = params.get("pass");

        if (user == null) {
            user = prompt();
        }
        if (pass == null) {
            pass = promptPassword();
        }

        try {
            Session.setDemo(false);
        } catch (Exception ignored) {
            // Ok
        }

        LoginResult res = dao.checkLogin(user, pass);

        if (res.status() == LoginStatus.SUCCESS) {
            CliSession.setAuthenticated(user, res.userId(), res.role());
            System.out.printf("✅ Login eseguito: %s (id=%d, ruolo=%s)%n", user, res.userId(), res.role());
        } else if (res.status() == LoginStatus.INVALID_CREDENTIALS) {
            System.out.println("❌ Credenziali non valide.");
            System.exit(1);
        } else {
            System.out.println("⚠️ Errore durante il login.");
            System.exit(2);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        String expecting = null;

        for (String arg : args) {
            if (expecting != null) {
                m.put(expecting, arg);
                expecting = null;
                continue;
            }
            switch (arg) {
                case "--user" -> expecting = "user";
                case "--pass" -> expecting = "pass";
                default -> { /* ignora token non riconosciuti */ }
            }
        }

        if (expecting != null) {
            throw new IllegalArgumentException("Manca il valore per --" + expecting);
        }
        return m;
    }

    private static String prompt() {
        System.out.print("Username: ");
        return System.console() == null
                ? new java.util.Scanner(System.in).nextLine()
                : System.console().readLine();
    }

    private static String promptPassword() {
        Console c = System.console();
        if (c != null) {
            char[] pwd = c.readPassword("Password: ");
            return pwd == null ? "" : new String(pwd);
        }
        System.out.print("Password: ");
        return new java.util.Scanner(System.in).nextLine();
    }
}
