package org.example.dao.fs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FsWishlist(String username, List<FsWishItem> items) {}
