package org.example.dao;

import org.example.dao.api.ProductDao;
import org.example.dao.db.ProductDaoDb;
import org.example.dao.fs.ProductDaoFs;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProductDaos {
    private ProductDaos(){}

    private static final Logger logger = Logger.getLogger(ProductDaos.class.getName());

    public static ProductDao create() {
        String mode = System.getProperty("persist.mode",
                System.getenv().getOrDefault("PERSIST_MODE", "DB"));
        if ("FS".equalsIgnoreCase(mode)) {
            String root = System.getProperty("fs.root", System.getenv("FS_ROOT")); // può essere null
            if (root == null || root.isBlank()) {
                logger.info(() -> "[DAO] Mode=FS (resources/data)");
                return new ProductDaoFs(); // legge da resources/data
            } else {
                logger.log(Level.INFO, () -> "[DAO] Mode=FS (folder) root=" + root);
                return new ProductDaoFs(java.nio.file.Path.of(root));
            }
        }
        logger.info("[DAO] Mode=DB");
        return new ProductDaoDb();
    }

}
