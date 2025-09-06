package org.example.dao.fs.model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FsWishItem(@JsonProperty("product_id") long productId, @JsonProperty("id_shop") int idShop,
                         @JsonProperty("p_size") String pSize) {}
