package org.example.control.services;

import org.example.dao.OrderDAO;
import org.example.gateway.PaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.CartItem;
import org.example.util.Session;
import org.example.util.CardValidator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentSelectionService {
    private final PaymentGateway gateway;
    private final Logger logger = Logger.getLogger(PaymentSelectionService.class.getName());
    private String lastOrderIds;

    public PaymentSelectionService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public List<CardViewModel> loadSavedCards(int userId) {
        try {
            return CardsService.loadSavedCards(userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento carte", e);
            return List.of();
        }
    }

    public PaymentResult confirmPayment(Card card, String cvv, String address,
                                        List<CartItem> items, BigDecimal total) {
        if (items == null || total == null) {
            return new PaymentResult(false, "Dati carrello non inizializzati", null, false);
        }
        if (card == null) {
            return new PaymentResult(false, "Carta non selezionata", null, false);
        }
        if (!CardValidator.isValidCvv(cvv)) {
            return new PaymentResult( false, "CVV non valido", null, false);
        }
        if (address == null || address.isBlank()) {
            return new PaymentResult(false, "Indirizzo spedizione mancante", null, false);
        }

        try {
            Map<String, String> paymentData = Map.of(
                    "card_number", card.number(),
                    "expiry", card.expiry(),
                    "cvv", cvv
            );

            logger.log(Level.FINE, "CVV presente: {0}", !cvv.isBlank() ? "***" : "no");

            PaymentResult payRes = gateway.charge(Session.getUserId(), total, paymentData);
            if (!payRes.success()) {
                return new PaymentResult(false, "Pagamento rifiutato: " + payRes.message(), payRes.transactionId(), false);
            }

            OrderDAO.CreationResult orderRes = OrderDAO.placeOrderWithStockDecrement(
                    Session.getUserId(), items, address);

            this.lastOrderIds = orderRes.orderIds().toString();
            logger.log(Level.INFO, "Payment txId: {0}", payRes.transactionId());

            return new PaymentResult(true, "Pagamento riuscito", payRes.transactionId(), false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore pagamento", e);
            return new PaymentResult(false, e.getMessage(), null, false);
        }
    }

    public String getLastOrderIds() {
        return lastOrderIds;
    }

}
