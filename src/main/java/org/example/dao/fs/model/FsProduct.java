package org.example.dao.fs.model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FsProduct(
        @JsonProperty("product_id") long productId,
        @JsonProperty("name_p") String nameP,
        String sport, String brand, String category,
        @JsonProperty("image_data_base64") String imageDataBase64,
        @JsonProperty("created_at") String createdAt
) {}
