package org.example.models.dto;

import java.math.BigDecimal;

public record ItemView(String productName, String size, int quantity, BigDecimal unitPrice, Object imageObj,
                       BigDecimal subtotal) {
}
