package org.example.cli;

import org.example.control.session.CartSession;
import org.example.dao.ProductRepository;
import org.example.models.entity.Product;
import org.example.models.entity.CartItem;

import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("java:S106")
final class CartCli {

    private final ProductRepository productDao;
    private final CartSession cartSession;
    private final ProductSearchSession searchSession;

    public CartCli(ProductRepository productDao, CartSession cartSession, ProductSearchSession searchSession) {
        this.productDao = productDao;
        this.cartSession = cartSession;
        this.searchSession = searchSession;
    }

    void handle(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (sub) {
                case "add" -> add(rest);
                case "show" -> show();
                case "clear" -> clear();
                default -> {
                    System.err.println("Sotto-comando cart sconosciuto: " + sub);
                    printHelp();
                }
            }
        } catch (Exception e) {
            System.err.println("Errore : " + e.getMessage());
            System.exit(2);
        }
    }

    private void printHelp() {
        System.out.println("""
            cart add  --id <productId> --shop <shopId> --size <size> [--qty <n>]
            cart show
            cart clear
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


    private void add(String[] args) {
        Map<String, String> p = parseArgs(args);
        Long productId = parseLong(p.get("id"));
        Integer shopId = parseInt(p.get("shop"));
        String size = p.get("size");
        Integer qty = parseInt(p.getOrDefault("qty", "1"));

        if (productId == null || shopId == null || size == null) {
            System.err.println("Specifica --id <productId>, --shop <shopId>, --size <size> [--qty <n>]");
            return;
        }
        if (qty == null || qty < 1) {
            qty = 1;
        }

        int stock = Optional.ofNullable(productDao.getStockFor(productId, shopId, size)).orElse(0);
        if (stock <= 0) {
            System.out.println("❌ Prodotto esaurito per la taglia " + size);
            return;
        }

        List<CartItem> currentItems = cartSession.getCartItems();
        int currentQty = currentItems.stream()
                .filter(i -> i.productId() == productId
                        && i.shopId() == shopId
                        && Objects.equals(i.size(), size))
                .mapToInt(CartItem::quantity)
                .sum();

        int finalQtyToAdd = Math.min(qty, stock - currentQty);
        if (finalQtyToAdd <= 0) {
            System.out.println("⚠️ Hai già raggiunto lo stock massimo disponibile per questo prodotto.");
            return;
        }

        if (finalQtyToAdd < qty) {
            System.out.println("⚠️ Quantità richiesta maggiore dello stock residuo. Imposto qty=" + finalQtyToAdd);
        }

        BigDecimal price = BigDecimal.valueOf(productDao.getPriceFor(productId, shopId, size));
        Product base = searchSession.find(productId, shopId).orElse(null);
        String name = base != null ? base.name() : ("(id " + productId + ")");

        CartItem added = new CartItem(
                productId,
                shopId,
                finalQtyToAdd,
                price,
                name,
                null,
                size
        );

        cartSession.addToCart(added);

        System.out.printf("🛒 Aggiunto al carrello: %s | size %s | qty %d | € %.2f cad.%n",
                name, size, finalQtyToAdd, price);
    }

    private void show() {
        List<CartItem> items = cartSession.getCartItems();
        if (items.isEmpty()) {
            System.out.println("Carrello vuoto.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;

        System.out.printf("%-10s %-8s %-28s %-6s %-8s %-10s%n",
                "productId", "shopId", "name", "size", "qty", "subtot");

        for (CartItem item : items) {
            BigDecimal unit = item.unitPrice() == null ? BigDecimal.ZERO : item.unitPrice();
            BigDecimal subtotal = unit.multiply(BigDecimal.valueOf(item.quantity()));
            total = total.add(subtotal);

            System.out.printf("%-10d %-8d %-28.28s %-6.6s %-8d %-10.2f%n",
                    item.productId(), item.shopId(), item.productName(), item.size(),
                    item.quantity(), subtotal.doubleValue());
        }

        System.out.printf("%nTotale: € %.2f%n", total);
    }

    private void clear() {
        cartSession.clearCart();
        System.out.println("Carrello svuotato.");
    }

    private static Long parseLong(String s) {
        try {
            return s == null? null: Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInt(String s){
        try {
            return s == null? null: Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}
