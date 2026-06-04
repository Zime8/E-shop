package org.example.models.dto;

public record CartRowData(ProductDto productDto, Aggregated agg, int stock, boolean stockError) { }

