package org.example.cli;

import org.example.control.session.CartSession;
import org.example.control.session.UserContext;
import org.example.dao.OrderRepository;
import org.example.dao.gateway.PaymentGateway;
import org.example.dao.gateway.PaymentGatewayException;
import org.example.dao.gateway.PaymentResult;
import org.example.models.entity.CartItem;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S106")
final class CheckoutCli {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final CartSession cartSession;
    private final UserContext userContext;

    public CheckoutCli(OrderRepository orderRepository,
                       PaymentGateway paymentGateway,
                       CartSession cartSession,
                       UserContext userContext) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.cartSession = cartSession;
        this.userContext = userContext;
    }

    void handle(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

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

    private void printHelp() {
        System.out.println("""
            checkout review
            checkout place --address "<indirizzo>" --card "<PAN>" --expiry "MM/YY" --cvv "123"
            """);
    }

    private Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        String pendingKey = null;

        for (String tok : args) {
            if (pendingKey != null) {
                params.put(pendingKey, tok);
                pendingKey = null;
                continue;
            }
            if (tok.startsWith("--")) {
                pendingKey = tok.substring(2);
            }
        }
        return params;
    }

    private void review() {
        List<CartItem> items = cartSession.getCartItems();
        if (items.isEmpty()) {
            System.out.println("Carrello vuoto.");
            return;
        }

        BigDecimal total = calculateTotal(items);

        System.out.printf("%-10s %-8s %-28s %-6s %-8s %-10s%n",
                "productId", "shopId", "name", "size", "qty", "subtot");

        for (CartItem item : items) {
            BigDecimal subtotal = calculateSubtotal(item);

            System.out.printf("%-10d %-8d %-28.28s %-6.6s %-8d %-10.2f%n",
                    item.productId(),
                    item.shopId(),
                    item.productName(),
                    item.size(),
                    item.quantity(),
                    subtotal.doubleValue());
        }

        System.out.printf("%nTotale: € %.2f%n", total);
    }

    private void place(String[] args) {
        List<CartItem> items = cartSession.getCartItems();
        if (items.isEmpty()) {
            System.out.println("Carrello vuoto: aggiungi prodotti prima del checkout.");
            return;
        }

        Map<String, String> params = parseArgs(args);
        String address = params.get("address");
        String pan = params.get("card");
        String expiry = params.get("expiry");
        String cvv = params.get("cvv");

        if (isBlank(address) || isBlank(pan) || isBlank(expiry) || isBlank(cvv)) {
            System.out.println("Parametri mancanti. Usa:\n  checkout place --address \"Via ...\" --card \"4111...\" --expiry \"MM/YY\" --cvv \"123\"");
            System.exit(1);
        }

        Integer userId = userContext.getCurrentUserId();
        if (userId == null) {
            System.out.println("Utente non autenticato.");
            System.exit(1);
            return;
        }

        BigDecimal total = calculateTotal(items);

        Map<String, String> payData = new HashMap<>();
        payData.put("card_number", pan);
        payData.put("expiry", expiry);
        payData.put("cvv", cvv);

        final PaymentResult paymentResult;
        try {
            paymentResult = paymentGateway.charge(userId, total, payData);
        } catch (PaymentGatewayException e) {
            System.out.println("Errore nel pagamento: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (!paymentResult.success()) {
            if (paymentResult.requiresAction()) {
                System.out.println("Pagamento richiede azione addizionale (3DS simulato). Riprova con un'altra carta.");
            } else {
                System.out.println("Pagamento rifiutato: " + paymentResult.message());
            }
            System.exit(2);
            return;
        }

        try {
            OrderRepository.CreationResult res =
                    orderRepository.placeOrderWithStockDecrement(userId, items, address);
            System.out.println("✅ Ordine creato. ID creati: " + res.orderIds());
            cartSession.clearCart();
        } catch (Exception e) {
            System.err.println("❌ Errore creazione ordine: " + e.getMessage());
            System.exit(3);
        }
    }

    private BigDecimal calculateTotal(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(calculateSubtotal(item));
        }
        return total;
    }

    private BigDecimal calculateSubtotal(CartItem item) {
        BigDecimal unit = item.unitPrice() == null ? BigDecimal.ZERO : item.unitPrice();
        return unit.multiply(BigDecimal.valueOf(item.quantity()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}