package org.example.cli;

import org.example.dao.UserDAO;
import org.example.dao.UserDAO.LoginResult;
import org.example.dao.UserDAO.LoginStatus;
import org.example.util.Session;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;

final class AuthCli {

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
        } catch (Throwable ignored) {
            // Ok
        }

        LoginResult res = UserDAO.checkLogin(user, pass);

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
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--user".equals(a) && i + 1 < args.length) {
                m.put("user", args[++i]);
            } else if ("--pass".equals(a) && i + 1 < args.length) {
                m.put("pass", args[++i]);
            }
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
