package org.example.cli;

import org.example.control.session.UserContext;
import org.example.dao.UserRepository;
import org.example.models.dto.LoginResult;
import org.example.models.dto.LoginStatus;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@SuppressWarnings("java:S106")
final class AuthCli {

    private final UserRepository userRepository;
    private final UserContext userContext;

    public AuthCli(UserRepository userRepository, UserContext userContext) {
        this.userRepository = userRepository;
        this.userContext = userContext;
    }

    void handleLoginCommand(String[] args) {
        Map<String, String> params = parseArgs(args);
        String user = params.get("user");
        String pass = params.get("pass");

        if (user == null) {
            user = prompt();
        }
        if (pass == null) {
            pass = promptPassword();
        }

        userContext.setDemo(false);

        LoginResult res = userRepository.checkLogin(user, pass);

        if (res.status() == LoginStatus.SUCCESS) {
            userContext.login(res.userId(), user);
            System.out.printf("✅ Login eseguito: %s (id=%d, ruolo=%s)%n",
                    user, res.userId(), res.role());
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
                default -> throw new IllegalArgumentException("Argomento non riconosciuto: " + arg);

            }
        }

        if (expecting != null) {
            throw new IllegalArgumentException("Manca il valore per --" + expecting);
        }
        return m;
    }

    private static String prompt() {
        System.out.print("Username: ");
        Console console = System.console();
        return console == null ? new Scanner(System.in).nextLine() : console.readLine();
    }

    private static String promptPassword() {
        Console c = System.console();
        if (c != null) {
            char[] pwd = c.readPassword("Password: ");
            return pwd == null ? "" : new String(pwd);
        }
        System.out.print("Password: ");
        return new Scanner(System.in).nextLine();
    }
}