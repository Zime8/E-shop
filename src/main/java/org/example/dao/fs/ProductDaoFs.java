package org.example.dao.fs;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.dao.ProductRepository;
import org.example.dao.fs.model.*;
import org.example.models.entity.Product;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ProductDaoFs implements ProductRepository {
    private final FsStore store;

    public ProductDaoFs(Path dataRoot) { this.store = new FsStore(dataRoot); }
    public ProductDaoFs() { this(null); }

    private static final String PRODUCTS = "products.json";
    private static final String AVAILABILITY = "product_availability.json";
    private static final String SHOPS = "shops.json";

    @Override
    public List<Product> findLatest(int limit) {
        store.rw.readLock().lock();
        try {
            var products = store.readList(PRODUCTS, new TypeReference<List<FsProduct>>() {});
            var avail    = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            var shops    = store.readList(SHOPS, new TypeReference<List<FsShop>>() {});

            Map<Long, Optional<FsAvailability>> minByProduct = avail.stream()
                    .collect(Collectors.groupingBy(FsAvailability::productId,
                            Collectors.minBy(Comparator.comparingDouble(FsAvailability::price))));
            Map<Integer, String> shopNames = shops.stream()
                    .collect(Collectors.toMap(FsShop::idShop, FsShop::nameS));

            Comparator<Product> cmp = Comparator
                    .comparing(Product::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Product::productId, Comparator.reverseOrder());

            return products.stream().map(fp -> {
                        var min = minByProduct.getOrDefault(fp.productId(), Optional.empty());
                        if (min.isEmpty()) return null;
                        var av = min.get();

                        return new Product(
                                fp.productId(),
                                av.idShop(),
                                fp.nameP(),
                                fp.sport(),
                                fp.brand(),
                                fp.category(),
                                shopNames.get(av.idShop()),
                                BigDecimal.valueOf(av.price()),
                                null,
                                null,
                                fp.createdAt() != null ? LocalDateTime.parse(fp.createdAt()) : null
                        );
                    }).filter(Objects::nonNull)
                    .sorted(cmp).limit(Math.max(0, limit)).toList();

        } finally { store.rw.readLock().unlock(); }
    }

    @Override
    public Optional<Product> findById(long productId) {
        store.rw.readLock().lock();
        try {
            var products = store.readList(PRODUCTS, new TypeReference<List<FsProduct>>() {});
            var avail    = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            var shops    = store.readList(SHOPS, new TypeReference<List<FsShop>>() {});

            Optional<FsProduct> fp = products.stream()
                    .filter(p -> p.productId() == productId).findFirst();  // ← FIX: productId()
            if (fp.isEmpty()) return Optional.empty();

            Optional<FsAvailability> av = avail.stream()
                    .filter(a -> a.productId() == productId)
                    .min(Comparator.comparingDouble(FsAvailability::price));
            if (av.isEmpty()) return Optional.empty();

            Map<Integer, String> shopNames = shops.stream()
                    .collect(Collectors.toMap(FsShop::idShop, FsShop::nameS));

            return Optional.of(new Product(
                    fp.get().productId(), av.get().idShop(), fp.get().nameP(),
                    fp.get().sport(), fp.get().brand(), fp.get().category(),
                    shopNames.getOrDefault(av.get().idShop(), ""),
                    BigDecimal.valueOf(av.get().price()), null, null,
                    fp.get().createdAt() != null ?
                            LocalDateTime.parse(fp.get().createdAt()) : null
            ));
        } finally {
            store.rw.readLock().unlock();
        }
    }

    @Override
    public List<Product> searchByName(String name) {
        store.rw.readLock().lock();
        try {
            String q = (name == null ? "" : name.toLowerCase());
            var products = store.readList(PRODUCTS, new TypeReference<List<FsProduct>>() {});
            var avail    = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            Map<Long, Double> minPrice = avail.stream().collect(
                    Collectors.groupingBy(FsAvailability::productId,
                            Collectors.mapping(FsAvailability::price,
                                    Collectors.collectingAndThen(Collectors.minBy(Double::compare), o -> o.orElse(0.0)))));

            return products.stream()
                    .filter(fp -> fp.nameP() != null && fp.nameP().toLowerCase().contains(q))
                    .map(fp -> new Product(
                            fp.productId(), 0, fp.nameP(), fp.sport(), fp.brand(),
                            fp.category(), "", BigDecimal.valueOf(minPrice.getOrDefault(fp.productId(), 0.0)),
                            null, null, fp.createdAt() != null ? LocalDateTime.parse(fp.createdAt()) : null
                    ))
                    .sorted(Comparator.comparing(Product::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Product::productId, Comparator.reverseOrder()))
                    .toList();
        } finally { store.rw.readLock().unlock(); }
    }

    @Override
    public List<Product> searchByFilters(String sport, String brand, String shop, String category,
                                         double minPrice, double maxPrice) {
        store.rw.readLock().lock();
        try {
            String sportVal = blankToNull(sport);
            String brandVal = blankToNull(brand);
            String catVal = blankToNull(category);
            String shopVal = blankToNull(shop);

            var products = store.readList(PRODUCTS, new TypeReference<List<FsProduct>>() {});
            var avail    = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            var shops    = store.readList(SHOPS, new TypeReference<List<FsShop>>() {});

            Map<Integer, String> shopNames = shops.stream()
                    .collect(Collectors.toMap(FsShop::idShop, FsShop::nameS));
            Map<String, Integer> shopIdByName = shops.stream()
                    .collect(Collectors.toMap(FsShop::nameS, FsShop::idShop));

            Integer shopId = shopVal == null ? null : shopIdByName.get(shopVal);

            var availFiltered = avail.stream()
                    .filter(a -> a.price() >= minPrice && a.price() <= maxPrice)
                    .filter(a -> shopId == null || a.idShop() == shopId)
                    .collect(Collectors.groupingBy(FsAvailability::productId,
                            Collectors.minBy(Comparator.comparingDouble(FsAvailability::price))));

            return products.stream()
                    .filter(p -> sportVal == null || Objects.equals(p.sport(), sportVal))
                    .filter(p -> brandVal == null || Objects.equals(p.brand(), brandVal))
                    .filter(p -> catVal == null || Objects.equals(p.category(), catVal))
                    .map(fp -> {
                        var min = availFiltered.getOrDefault(fp.productId(), Optional.empty());
                        if (min.isEmpty()) return null;
                        var a = min.get();
                        return new Product(
                                fp.productId(), a.idShop(), fp.nameP(), fp.sport(), fp.brand(),
                                fp.category(), shopNames.get(a.idShop()),
                                BigDecimal.valueOf(a.price()), null, null,
                                fp.createdAt() != null ? LocalDateTime.parse(fp.createdAt()) : null
                        );
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Product::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Product::productId, Comparator.reverseOrder()))
                    .toList();
        } finally { store.rw.readLock().unlock(); }
    }

    @Override public int getShopIdByName(String shopName) {
        store.rw.readLock().lock();
        try {
            var shops = store.readList(SHOPS, new TypeReference<List<FsShop>>() {});
            return shops.stream()
                    .filter(s -> s.nameS().equals(shopName))
                    .findFirst().map(FsShop::idShop)
                    .orElseThrow(() -> new RuntimeException("Shop not found: " + shopName));
        } finally { store.rw.readLock().unlock(); }
    }

    @Override public List<String> getAvailableSizes(long productId, int idShop) {
        store.rw.readLock().lock();
        try {
            var avail = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            return avail.stream()
                    .filter(a -> a.productId()==productId && a.idShop()==idShop && a.quantity()>0)
                    .map(FsAvailability::size).distinct().sorted().toList();
        } finally { store.rw.readLock().unlock(); }
    }

    @Override public double getPriceFor(long productId, int idShop, String size) {
        store.rw.readLock().lock();
        try {
            var avail = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            return avail.stream()
                    .filter(a -> a.productId()==productId && a.idShop()==idShop && Objects.equals(a.size(), size))
                    .map(FsAvailability::price).findFirst()
                    .orElseThrow(() -> new RuntimeException("Prezzo non trovato"));
        } finally { store.rw.readLock().unlock(); }
    }

    @Override public Integer getStockFor(long productId, int shopId, String size) {
        store.rw.readLock().lock();
        try {
            var avail = store.readList(AVAILABILITY, new TypeReference<List<FsAvailability>>() {});
            return avail.stream()
                    .filter(a -> a.productId()==productId && a.idShop()==shopId && Objects.equals(a.size(), size))
                    .map(FsAvailability::quantity).findFirst().orElse(0);
        } finally { store.rw.readLock().unlock(); }
    }

    @Override public boolean existsWish(String username, long productId, int shopId, String size) {
        store.rw.readLock().lock();
        try {
            var all = store.readList("wishlist.json", new TypeReference<List<FsWishlist>>() {});
            return all.stream()
                    .filter(w -> w.username().equals(username))
                    .flatMap(w -> w.items().stream())
                    .anyMatch(i -> i.productId()==productId && i.idShop()==shopId &&
                            (size==null || Objects.equals(i.pSize(), size)));
        } finally { store.rw.readLock().unlock(); }
    }

    private static String blankToNull(String s){ return (s==null || s.isBlank()) ? null : s; }
}
