package org.example.cli;

import org.example.dao.ProductRepository;
import org.example.models.entity.Product;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S106")
final class ProductCli {

    private final ProductRepository productDao;
    private final ProductSearchSession searchSession;

    ProductCli(ProductRepository productDao, ProductSearchSession searchSession) {
        this.productDao = productDao;
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
                case "search" -> search(rest);
                case "sizes" -> sizes(rest);
                case "price" -> price(rest);
                default -> {
                    System.err.println("Sotto-comando product sconosciuto: " + sub);
                    printHelp();
                }
            }
        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
            System.exit(2);
        }
    }

    private void printHelp() {
        System.out.println("""
            product search --name "<testo>"
            product sizes  --id <productId> --shop <shopId>
            product price  --id <productId> --shop <shopId> --size <size>
            """);
    }

    private Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        String expecting = null;

        for (String token : args) {
            if (expecting != null) {
                params.put(expecting, token);
                expecting = null;
                continue;
            }

            if (token.startsWith("--")) {
                int eq = token.indexOf('=');
                if (eq > 2) {
                    params.put(token.substring(2, eq), token.substring(eq + 1));
                } else {
                    expecting = token.substring(2);
                }
            }
        }

        return params;
    }

    private void search(String[] args) {
        Map<String, String> params = parseArgs(args);
        String name = params.get("name");

        if (name == null || name.isBlank()) {
            System.err.println("Specifica --name \"testo\"");
            return;
        }

        List<Product> found = productDao.searchByName(name);
        searchSession.replaceAll(found);

        if (found.isEmpty()) {
            System.out.println("Nessun prodotto trovato per: " + name);
            return;
        }

        System.out.println("Trovati " + found.size() + " prodotti:");
        System.out.printf("%-10s %-8s %-28s %-14s %-10s %-8s%n",
                "productId", "shopId", "name", "brand", "price", "sport");

        for (Product product : found) {
            System.out.printf("%-10d %-8d %-28.28s %-14.14s %-10.2f %-8.8s%n",
                    product.productId(),
                    product.idShop(),
                    nullSafe(product.name()),
                    nullSafe(product.brand()),
                    product.price(),
                    nullSafe(product.sport()));
        }

        System.out.println("\nUsa: cart add --id <productId> --shop <shopId> --size <size> [--qty <n>]");
    }

    private void sizes(String[] args) {
        Map<String, String> params = parseArgs(args);
        Long productId = parseLong(params.get("id"));
        Integer shopId = parseInt(params.get("shop"));

        if (productId == null || shopId == null) {
            System.err.println("Specifica --id <productId> e --shop <shopId>");
            return;
        }

        List<String> sizes = productDao.getAvailableSizes(productId, shopId);
        if (sizes.isEmpty()) {
            System.out.println("Nessuna taglia disponibile.");
        } else {
            System.out.println("Taglie disponibili: " + String.join(", ", sizes));
        }
    }

    private void price(String[] args) {
        Map<String, String> params = parseArgs(args);
        Long productId = parseLong(params.get("id"));
        Integer shopId = parseInt(params.get("shop"));
        String size = params.get("size");

        if (productId == null || shopId == null || size == null) {
            System.err.println("Specifica --id <productId>, --shop <shopId> e --size <size>");
            return;
        }

        double price = productDao.getPriceFor(productId, shopId, size);
        Integer stock = productDao.getStockFor(productId, shopId, size);

        System.out.printf("Prezzo: € %.2f | Stock: %s%n",
                price, stock == null ? "—" : stock);
    }

    private static String nullSafe(String s) {
        return s == null ? "-" : s;
    }

    private static Long parseLong(String s) {
        try {
            return s == null ? null : Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        try {
            return s == null ? null : Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}