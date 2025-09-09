package org.example.dao.fs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

final class FsStore {
    private static final Logger logger = Logger.getLogger(FsStore.class.getName());

    private final Path root;
    private final ObjectMapper om = new ObjectMapper();
    final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    FsStore(Path root){ this.root = root; }

    <T> List<T> readList(String file, TypeReference<List<T>> type) {
        rw.readLock().lock();
        try {
            if (root != null) {
                Path p = root.resolve(file);
                if (!Files.exists(p)) {
                    logger.fine(() -> "File non trovato nel FS: " + p.toAbsolutePath());
                    return List.of();
                }
                try (InputStream in = Files.newInputStream(p)) {
                    return om.readValue(in, type);
                } catch (IOException e) {
                    logger.log(Level.WARNING, e,
                            () -> "Errore IO/parsing leggendo '" + p.toAbsolutePath() + "'");
                    return List.of();
                }
            } else {
                String resPath = "data/" + file;
                try (InputStream in = getClass().getClassLoader().getResourceAsStream(resPath)) {
                    if (in == null) {
                        logger.fine(() -> "Resource non trovata: " + resPath);
                        return List.of();
                    }
                    return om.readValue(in, type);
                } catch (IOException e) {
                    logger.log(Level.WARNING, e,
                            () -> "Errore IO/parsing leggendo resource '" + resPath + "'");
                    return List.of();
                }
            }
        } finally {
            rw.readLock().unlock();
        }
    }
}
