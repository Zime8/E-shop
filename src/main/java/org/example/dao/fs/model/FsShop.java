package org.example.dao.fs.model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FsShop(@JsonProperty("id_shop") int idShop, @JsonProperty("name_s") String nameS) {}
