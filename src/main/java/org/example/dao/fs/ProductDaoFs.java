package org.example.dao.fs;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.dao.api.ProductDao;
import org.example.dao.fs.model.*;
import org.example.models.Product;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ProductDaoFs implements ProductDao {
    private final FsStore store;

    public ProductDaoFs(Path dataRoot) { this.store = new FsStore(dataRoot); }
    public ProductDaoFs() { this(null); } // usa resources/data

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
                    .comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Product::getProductId, Comparator.reverseOrder());

            return products.stream().map(fp -> {
                var p = new Product();
                p.setProductId(fp.productId());
                p.setName(fp.nameP());
                p.setSport(fp.sport());
                p.setBrand(fp.brand());
                p.setCategory(fp.category());
                if (fp.createdAt() != null) p.setCreatedAt(LocalDateTime.parse(fp.createdAt()));
                var min = minByProduct.getOrDefault(fp.productId(), Optional.empty());
                min.ifPresent(av -> {
                    p.setPrice(av.price());
                    p.setIdShop(av.idShop());
                    p.setNameShop(shopNames.get(av.idShop()));
                });
                return p;
            }).sorted(cmp).limit(Math.max(0, limit)).toList();
        } finally { store.rw.readLock().unlock(); }
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
                    .filter(fp -> fp.nameP()!=null && fp.nameP().toLowerCase().contains(q))
                    .map(fp -> {
                        var p = new Product();
                        p.setProductId(fp.productId());
                        p.setName(fp.nameP());
                        p.setSport(fp.sport());
                        p.setBrand(fp.brand());
                        p.setCategory(fp.category());
                        p.setPrice(minPrice.getOrDefault(fp.productId(), 0.0));
                        if (fp.createdAt()!=null) p.setCreatedAt(LocalDateTime.parse(fp.createdAt()));
                        return p;
                    })
                    .sorted(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Product::getProductId, Comparator.reverseOrder()))
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
                    .filter(p -> catVal  == null || Objects.equals(p.category(), catVal))
                    .map(fp -> {
                        var min = availFiltered.getOrDefault(fp.productId(), Optional.empty());
                        if (min.isEmpty()) return null;
                        var a = min.get();
                        var p = new Product();
                        p.setProductId(fp.productId());
                        p.setName(fp.nameP());
                        p.setSport(fp.sport());
                        p.setBrand(fp.brand());
                        p.setCategory(fp.category());
                        p.setPrice(a.price());
                        p.setIdShop(a.idShop());
                        p.setNameShop(shopNames.get(a.idShop()));
                        if (fp.createdAt()!=null) p.setCreatedAt(LocalDateTime.parse(fp.createdAt()));
                        return p;
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Product::getProductId, Comparator.reverseOrder()))
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
