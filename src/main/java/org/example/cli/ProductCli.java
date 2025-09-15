package org.example.cli;

import org.example.dao.api.ProductDao;
import org.example.dao.db.ProductDaoDb;
import org.example.models.Product;

import java.sql.SQLException;
import java.util.*;

final class ProductCli {

    private static final ProductDao dao = new ProductDaoDb();

    // Tiene in memoria gli ultimi risultati di ricerca per facilitare la selezione
    private static final List<Product> lastSearch = new ArrayList<>();

    private ProductCli() {}

    static void handle(String[] args) {
        if (args.length == 0) { printHelp(); return; }
        String sub = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (sub) {
                case "search" -> search(rest);
                case "sizes"  -> sizes(rest);
                case "price"  -> price(rest);
                default -> {
                    System.err.println("Sotto-comando product sconosciuto: " + sub);
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
            product search --name "<testo>"
            product sizes  --id <productId> --shop <shopId>
            product price  --id <productId> --shop <shopId> --size <size>
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

    private static void search(String[] args) throws SQLException {
        Map<String, String> p = parseArgs(args);
        String name = p.get("name");
        if (name == null || name.isBlank()) {
            System.err.println("Specifica --name \"testo\"");
            return;
        }

        List<Product> found = dao.searchByName(name);
        lastSearch.clear();
        lastSearch.addAll(found);

        if (found.isEmpty()) {
            System.out.println("Nessun prodotto trovato per: " + name);
            return;
        }

        // Stampa una tabella di prodotto
        System.out.println("Trovati " + found.size() + " prodotti:");
        System.out.printf("%-10s %-8s %-28s %-14s %-10s %-8s%n",
                "productId","shopId","name","brand","price","sport");
        for (Product pr : found) {
            System.out.printf("%-10d %-8d %-28.28s %-14.14s %-10.2f %-8.8s%n",
                    pr.getProductId(), pr.getIdShop(), nullSafe(pr.getName()),
                    nullSafe(pr.getBrand()), pr.getPrice(), nullSafe(pr.getSport()));
        }
        System.out.println("\nUsa: cart add --id <productId> --shop <shopId> --size <size> [--qty <n>]");
    }

    private static void sizes(String[] args) throws SQLException {
        Map<String, String> p = parseArgs(args);
        Long pid = parseLong(p.get("id"));
        Integer shop = parseInt(p.get("shop"));
        if (pid == null || shop == null) {
            System.err.println("Specifica --id <productId> e --shop <shopId>");
            return;
        }
        List<String> sizes = dao.getAvailableSizes(pid, shop);
        if (sizes.isEmpty()) {
            System.out.println("Nessuna taglia disponibile.");
        } else {
            System.out.println("Taglie disponibili: " + String.join(", ", sizes));
        }
    }

    private static void price(String[] args) throws SQLException {
        Map<String, String> p = parseArgs(args);
        Long pid = parseLong(p.get("id"));
        Integer shop = parseInt(p.get("shop"));
        String size = p.get("size");
        if (pid == null || shop == null || size == null) {
            System.err.println("Specifica --id <productId>, --shop <shopId> e --size <size>");
            return;
        }
        double price = dao.getPriceFor(pid, shop, size);
        Integer stock = dao.getStockFor(pid, shop, size);
        System.out.printf("Prezzo: € %.2f | Stock: %s%n", price, stock == null ? "—" : stock);
    }

    static Optional<Product> findInLastSearch(long productId, int shopId) {
        return lastSearch.stream()
                .filter(p -> p.getProductId() == productId && p.getIdShop() == shopId)
                .findFirst();
    }

    private static String nullSafe(String s) { return s == null ? "-" : s; }
    private static Long parseLong(String s) { try { return s == null? null: Long.parseLong(s); } catch(Exception e){ return null; } }
    private static Integer parseInt(String s){ try { return s == null? null: Integer.parseInt(s);} catch(Exception e){ return null; } }
}
