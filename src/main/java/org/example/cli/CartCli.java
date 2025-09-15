package org.example.cli;

import org.example.dao.api.ProductDao;
import org.example.dao.db.ProductDaoDb;
import org.example.models.Product;
import org.example.models.CartItem;

import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("java:S106")
final class CartCli {

    private static final ProductDao dao = new ProductDaoDb();

    // Stato carrello in memoria
    private static final List<CartItem> cart = new ArrayList<>();

    private CartCli() {}

    static void handle(String[] args) {
        if (args.length == 0) { printHelp(); return; }
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
        } catch (SQLException e) {
            System.err.println("Errore DB: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void printHelp() {
        System.out.println("""
            cart add  --id <productId> --shop <shopId> --size <size> [--qty <n>]
            cart show
            cart clear
            """);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i=0; i<args.length; i++) {
            String a = args[i];
            if (a.startsWith("--") && i+1 < args.length) {
                m.put(a.substring(2), args[++i]);
            }
        }
        return m;
    }

    private static void add(String[] args) throws SQLException {
        Map<String, String> p = parseArgs(args);
        Long pid = parseLong(p.get("id"));
        Integer shop = parseInt(p.get("shop"));
        String size = p.get("size");
        Integer qty = parseInt(p.getOrDefault("qty", "1"));

        if (pid == null || shop == null || size == null) {
            System.err.println("Specifica --id <productId>, --shop <shopId>, --size <size> [--qty <n>]");
            return;
        }
        if (qty == null || qty < 1) qty = 1;

        // Stock e prezzo correnti dal DB
        int stock = Optional.ofNullable(dao.getStockFor(pid, shop, size)).orElse(0);
        if (stock <= 0) {
            System.out.println("❌ Prodotto esaurito per la taglia " + size);
            return;
        }
        if (qty > stock) {
            System.out.println("⚠️ Quantità richiesta maggiore dello stock. Imposto qty=" + stock);
            qty = stock;
        }
        double price = dao.getPriceFor(pid, shop, size);

        // Prova a recuperare nome/brand/shop dall'ultima search solo per stampa
        Product base = ProductCli.findInLastSearch(pid, shop).orElse(null);
        String name = base != null ? base.getName() : ("(id " + pid + ")");

        // Cerca item esistente
        int idx = -1;
        for (int i = 0; i < cart.size(); i++) {
            CartItem it = cart.get(i);
            if (it.getProductId() == pid && it.getShopId() == shop && Objects.equals(it.getSize(), size)) {
                idx = i; break;
            }
        }

        if (idx >= 0) {
            CartItem old = cart.get(idx);
            int newQty = Math.min(stock, old.getQuantity() + qty);
            // crea nuova istanza con quantità aggiornata e prezzo corrente
            CartItem updated = new CartItem(pid, shop, newQty, price, old.getProductName(), old.getProductImage(), size);
            cart.set(idx, updated);
        } else {
            // nuova riga carrello
            CartItem added = new CartItem(pid, shop, qty, price, name, null, size);
            cart.add(added);
        }

        System.out.printf("🛒 Aggiunto al carrello: %s | size %s | qty %d | € %.2f cad.%n",
                name, size, qty, price);
    }

    private static void show() {
        if (cart.isEmpty()) {
            System.out.println("Carrello vuoto.");
            return;
        }
        double total = 0.0;
        System.out.printf("%-10s %-8s %-28s %-6s %-8s %-10s%n",
                "productId","shopId","name","size","qty","subtot");
        for (CartItem it : cart) {
            double unit = it.getUnitPrice() == null ? 0.0 : it.getUnitPrice();
            double sub = unit * it.getQuantity();
            total += sub;
            System.out.printf("%-10d %-8d %-28.28s %-6.6s %-8d %-10.2f%n",
                    it.getProductId(), it.getShopId(), it.getProductName(), it.getSize(),
                    it.getQuantity(), sub);
        }
        System.out.printf("%nTotale: € %.2f%n", total);
    }

    private static void clear() {
        cart.clear();
        System.out.println("Carrello svuotato.");
    }

    // Getter per il prossimo step (checkout)
    static List<CartItem> items() { return Collections.unmodifiableList(cart); }

    public static void clearAll() {
        cart.clear();
    }

    private static Long parseLong(String s) { try { return s == null? null: Long.parseLong(s); } catch(Exception e){ return null; } }
    private static Integer parseInt(String s){ try { return s == null? null: Integer.parseInt(s);} catch(Exception e){ return null; } }
}
