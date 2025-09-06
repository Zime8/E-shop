package org.example.dao.fs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class FsStore {
    private final Path root;                 // se root è null -> resources
    private final ObjectMapper om = new ObjectMapper();
    final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    FsStore(Path root){ this.root = root; }

    <T> List<T> readList(String file, TypeReference<List<T>> type) {
        try {
            if (root != null) {
                Path p = root.resolve(file);
                if (!Files.exists(p)) return List.of();
                try (var in = Files.newInputStream(p)) { return om.readValue(in, type); }
            } else {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/" + file)) {
                    if (in == null) return List.of();
                    return om.readValue(in, type);
                }
            }
        } catch (Exception e) { throw new RuntimeException("Errore lettura "+file, e); }
    }
}
