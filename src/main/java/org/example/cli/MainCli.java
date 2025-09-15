package org.example.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S106")
public final class MainCli {

    private static final String PROMPT = "eshop> ";

    private MainCli() {}

    private static void printHelp() {
        System.out.println("""
            E-Shop CLI
            Comandi:
              help
              login [--user <username>] [--pass <password>]

              (Disponibili SOLO dopo il login:)
              product search --name "<testo>"
              product sizes  --id <productId> --shop <shopId>
              product price  --id <productId> --shop <shopId> --size <size>

              cart add   --id <productId> --shop <shopId> --size <size> [--qty <n>]
              cart show
              cart clear

              checkout review
              checkout place --address "<indirizzo>" --card "<PAN>" --expiry "MM/YY" --cvv "123"

            Suggerimenti:
              • Usa virgolette per argomenti con spazi, es: --name "air max"
              • Esci con 'exit' o 'quit'
            """);
    }

    public static void main(String[] args) {
        // Modalità interattiva: nessun argomento
        if (args.length == 0) {
            printHelp();
            interactiveShell();
            return;
        }

        // Modalità batch: un singolo comando passato da riga di comando
        int code = executeOnce(args /*interactive=*/);
        if (code != 0) System.exit(code);
    }

    // legge comandi da stdin e li esegue finché l'utente non esce
    private static void interactiveShell() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            boolean running = true;
            while (running) {
                System.out.print(PROMPT);
                String line = br.readLine();

                if (line == null) {
                    running = false;
                } else {
                    line = line.trim();
                    if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                        running = false;
                    } else if (!line.isEmpty()) {
                        String[] argv = splitArgs(line);
                        if (argv.length > 0) {
                            executeOnce(argv);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Errore I/O: " + e.getMessage());
        }
        System.out.println("Bye!");
    }

    // Esegue un singolo comando, restituendo un exit code
    private static int executeOnce(String[] args) {
        String cmd = args[0].toLowerCase();
        String[] rest = slice(args);

        // sempre permessi
        if ("help".equals(cmd) || "-h".equals(cmd) || "--help".equals(cmd)) {
            printHelp();
            return 0;
        }
        if ("login".equals(cmd)) {
            AuthCli.handleLoginCommand(rest);
            return 0;
        }

        // serve login per tutto il resto
        if (!CliSession.isAuthenticated()) {
            System.out.println("Devi prima effettuare il login:\n  login --user <username> --pass <password>");
            return 1;
        }

        switch (cmd) {
            case "product"  -> ProductCli.handle(rest);
            case "cart"     -> CartCli.handle(rest);
            case "checkout" -> CheckoutCli.handle(rest);
            default -> {
                System.err.println("Comando sconosciuto: " + cmd);
                printHelp();
                return 2;
            }
        }
        return 0;
    }

    // Split degli argomenti rispettando virgolette e backslash
    private static String[] splitArgs(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        boolean escape = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escape) {
                cur.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (!cur.isEmpty()) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) {
            tokens.add(cur.toString());
        }
        return tokens.toArray(new String[0]);
    }

    private static String[] slice(String[] arr) {
        if (arr.length <= 1) return new String[0];
        String[] out = new String[arr.length - 1];
        System.arraycopy(arr, 1, out, 0, out.length);
        return out;
    }
}
