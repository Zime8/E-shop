package org.example.cli;

import org.example.dao.OrderDAO;
import org.example.gateway.FakePaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.CartItem;
import org.example.gateway.PaymentGatewayException;

import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("java:S106")
final class CheckoutCli {

    private CheckoutCli() {}

    static void handle(String[] args) {
        if (args.length == 0) { printHelp(); return; }
        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (sub) {
            case "review" -> review();
            case "place"  -> place(rest);
            default -> {
                System.err.println("Sotto-comando checkout sconosciuto: " + sub);
                printHelp();
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
            checkout review
            checkout place --address "<indirizzo>" --card "<PAN>" --expiry "MM/YY" --cvv "123"
            """);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        String pendingKey = null;

        for (String tok : args) {
            if (pendingKey != null) {
                m.put(pendingKey, tok);
                pendingKey = null;
                continue;
            }
            if (tok.startsWith("--")) {
                pendingKey = tok.substring(2);
            }
        }
        return m;
    }

    private static void review() {
        List<CartItem> items = CartCli.items();
        if (items.isEmpty()) {
            System.out.println("Carrello vuoto.");
            return;
        }

        double total = 0.0;
        System.out.printf("%-10s %-8s %-28s %-6s %-8s %-10s%n",
                "productId","shopId","name","size","qty","subtot");

        for (CartItem it : items) {
            double unit = it.getUnitPrice() == null ? 0.0 : it.getUnitPrice();
            int qty = it.getQuantity();
            double sub = unit * qty;
            total += sub;

            System.out.printf("%-10d %-8d %-28.28s %-6.6s %-8d %-10.2f%n",
                    it.getProductId(), it.getShopId(), it.getProductName(), it.getSize(), qty, sub);
        }
        System.out.printf("%nTotale: € %.2f%n", total);
    }

    private static void place(String[] args) {
        List<CartItem> items = CartCli.items();
        if (items.isEmpty()) {
            System.out.println("Carrello vuoto: aggiungi prodotti prima del checkout.");
            return;
        }

        Map<String, String> p = parseArgs(args);
        String address = p.get("address");
        String pan     = p.get("card");
        String expiry  = p.get("expiry");
        String cvv     = p.get("cvv");

        if (isBlank(address) || isBlank(pan) || isBlank(expiry) || isBlank(cvv)) {
            System.out.println("Parametri mancanti. Usa:\n  checkout place --address \"Via ...\" --card \"4111...\" --expiry \"MM/YY\" --cvv \"123\"");
            System.exit(1);
        }

        // Calcolo totale
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem it : items) {
            double unit = it.getUnitPrice() == null ? 0.0 : it.getUnitPrice();
            total = total.add(BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(it.getQuantity())));
        }

        // Pagamento "fake"
        var pay = new FakePaymentGateway(800, 0.10);
        Map<String,String> payData = new HashMap<>();
        payData.put("card_number", pan);
        payData.put("expiry", expiry);
        payData.put("cvv", cvv);

        int userId = Optional.ofNullable(CliSession.userId()).orElseThrow();
        final PaymentResult pr;
        try {
            pr = pay.charge(userId, total, payData);
        } catch (PaymentGatewayException e) {
            System.out.println("Errore nel pagamento: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (!pr.success()) {
            if (pr.requiresAction()) {
                System.out.println("Pagamento richiede azione addizionale (3DS simulato). Riprova con un'altra carta.");
            } else {
                System.out.println("Pagamento rifiutato: " + pr.message());
            }
            System.exit(2);
        }

        try {
            OrderDAO.CreationResult res = OrderDAO.placeOrderWithStockDecrement(userId, items, address);
            System.out.println("✅ Ordine creato. ID creati: " + res.orderIds());
            CartCli.clearAll();
        } catch (Exception e) {
            System.err.println("❌ Errore creazione ordine: " + e.getMessage());
            System.exit(3);
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
