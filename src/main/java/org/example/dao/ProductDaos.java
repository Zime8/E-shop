package org.example.dao;

import org.example.dao.api.ProductDao;
import org.example.dao.db.ProductDaoDb;
import org.example.dao.fs.ProductDaoFs;

public final class ProductDaos {
    private ProductDaos(){}

    public static ProductDao create() {
        String mode = System.getProperty("persist.mode",
                System.getenv().getOrDefault("PERSIST_MODE", "DB"));
        if ("FS".equalsIgnoreCase(mode)) {
            String root = System.getProperty("fs.root", System.getenv("FS_ROOT")); // può essere null
            if (root == null || root.isBlank()) {
                System.out.println("[DAO] Mode=FS (resources/data)");
                return new ProductDaoFs(); // legge da resources/data
            } else {
                System.out.println("[DAO] Mode=FS (folder) root=" + root);
                return new ProductDaoFs(java.nio.file.Path.of(root));
            }
        }
        System.out.println("[DAO] Mode=DB");
        return new ProductDaoDb();
    }

}
