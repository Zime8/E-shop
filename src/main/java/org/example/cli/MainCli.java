package org.example.cli;

import org.example.control.session.CartSession;
import org.example.control.session.SessionCartSession;
import org.example.control.session.SessionUserContext;
import org.example.control.session.UserContext;
import org.example.dao.OrderRepository;
import org.example.dao.ProductDaos;
import org.example.dao.ProductRepository;
import org.example.dao.UserRepository;
import org.example.dao.db.DbOrderDAO;
import org.example.dao.db.DbUserDAO;
import org.example.dao.gateway.FakePaymentGateway;
import org.example.dao.gateway.PaymentGateway;
import org.example.util.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("java:S106")
public final class MainCli {

    private static final String PROMPT = "eshop> ";
    private final CheckoutCli checkoutCli;
    private final AuthCli authCli;
    private final CartCli cartCli;
    private final ProductCli productCli;
    private final UserContext userContext;

    MainCli(CheckoutCli checkoutCli,
            AuthCli authCli,
            CartCli cartCli,
            ProductCli productCli,
            UserContext userContext) {
        this.checkoutCli = checkoutCli;
        this.authCli = authCli;
        this.cartCli = cartCli;
        this.productCli = productCli;
        this.userContext = userContext;
    }

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
        MainCli app = bootstrap();

        if (args.length == 0) {
            printHelp();
            app.interactiveShell();
            return;
        }

        int code = app.executeOnce(args);
        if (code != 0) System.exit(code);
    }

    private static MainCli bootstrap() {
        UserRepository userRepository = new DbUserDAO();
        ProductRepository productDao = ProductDaos.create();
        OrderRepository orderRepository = new DbOrderDAO();
        PaymentGateway paymentGateway = new FakePaymentGateway(800, 0.10);

        Session session = new Session();
        UserContext userContext = new SessionUserContext(session);
        CartSession cartSession = new SessionCartSession(session);
        ProductSearchSession searchSession = new ProductSearchSession();

        AuthCli authCli = new AuthCli(userRepository, userContext);
        ProductCli productCli = new ProductCli(productDao, searchSession);
        CartCli cartCli = new CartCli(productDao, cartSession, searchSession);
        CheckoutCli checkoutCli = new CheckoutCli(orderRepository, paymentGateway, cartSession, userContext);

        return new MainCli(checkoutCli, authCli, cartCli, productCli, userContext);
    }

    private void interactiveShell() {
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

    private int executeOnce(String[] args) {
        String cmd = args[0].toLowerCase();
        String[] rest = slice(args);

        if ("help".equals(cmd) || "-h".equals(cmd) || "--help".equals(cmd)) {
            printHelp();
            return 0;
        }
        if ("login".equals(cmd)) {
            authCli.handleLoginCommand(rest);
            return 0;
        }

        if (!userContext.isLoggedIn()) {
            System.out.println("Devi prima effettuare il login:\n  login --user <username> --pass <password>");
            return 1;
        }

        switch (cmd) {
            case "product"  -> productCli.handle(rest);
            case "cart"     -> cartCli.handle(rest);
            case "checkout" -> checkoutCli.handle(rest);
            default -> {
                System.err.println("Comando sconosciuto: " + cmd);
                printHelp();
                return 2;
            }
        }
        return 0;
    }

    private static String[] splitArgs(String line) {
        List<String> tokens = new ArrayList<>();
        ArgLexer lx = new ArgLexer();
        boolean escape = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escape) {
                lx.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                lx.onChar(c, tokens);
            }
        }
        lx.finish(tokens);
        return tokens.toArray(new String[0]);
    }

    private static final class ArgLexer {
        private final StringBuilder cur = new StringBuilder();
        private boolean inQuotes = false;
        private char quoteChar = 0;

        void onChar(char c, List<String> out) {
            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    cur.append(c);
                }
                return;
            }

            if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
                return;
            }

            if (Character.isWhitespace(c)) {
                flush(out);
                return;
            }

            cur.append(c);
        }

        void append(char c) {
            cur.append(c);
        }

        void finish(List<String> out) {
            flush(out);
        }

        private void flush(List<String> out) {
            if (!cur.isEmpty()) {
                out.add(cur.toString());
                cur.setLength(0);
            }
        }
    }

    private static String[] slice(String[] arr) {
        if (arr.length <= 1) return new String[0];
        String[] out = new String[arr.length - 1];
        System.arraycopy(arr, 1, out, 0, out.length);
        return out;
    }
}