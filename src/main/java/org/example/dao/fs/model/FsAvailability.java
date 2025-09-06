package org.example.dao.fs.model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FsAvailability(@JsonProperty("product_id") long productId, @JsonProperty("id_shop") int idShop,
                             String size, double price, int quantity) {}
