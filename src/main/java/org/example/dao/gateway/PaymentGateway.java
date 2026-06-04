package org.example.dao.gateway;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {

    PaymentResult charge(int userId, BigDecimal amount, Map<String,String> paymentData)
            throws PaymentGatewayException;

}
