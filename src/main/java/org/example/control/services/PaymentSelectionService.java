package org.example.control.services;

import org.example.dao.OrderRepository;
import org.example.dao.gateway.PaymentGateway;
import org.example.dao.gateway.PaymentResult;
import org.example.models.dto.Card;
import org.example.models.dto.CheckoutResult;
import org.example.models.dto.InlineCardData;
import org.example.models.entity.CartItem;
import org.example.util.CardValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentSelectionService {
    private final PaymentGateway gateway;
    private final CardsService cardsService;
    private final OrderRepository orderRepository;
    private final Logger logger = Logger.getLogger(PaymentSelectionService.class.getName());

    public PaymentSelectionService(PaymentGateway gateway, CardsService cardsService, OrderRepository orderRepository) {
        this.gateway = gateway;
        this.cardsService = cardsService;
        this.orderRepository = orderRepository;
    }

    public List<Card> loadSavedCards(int userId) {
        return cardsService.loadSavedCards(userId);
    }

    public CardsService.AddCardResult addInlineCard(int userId, InlineCardData data) {
        return cardsService.addInlineCard(userId, data);
    }

    public CheckoutResult confirmPayment(Card card, String cvv, String address,
                                         List<CartItem> items, BigDecimal total, Integer userId) {
        if (userId == null) {
            return new CheckoutResult(false, "Utente non valido", null, null);
        }
        if (items == null || total == null) {
            return new CheckoutResult(false, "Dati carrello non inizializzati", null, null);
        }
        if (card == null) {
            return new CheckoutResult(false, "Carta non selezionata", null, null);
        }
        if (!CardValidator.isValidCvv(cvv)) {
            return new CheckoutResult( false, "CVV non valido", null, null);
        }
        if (address == null || address.isBlank()) {
            return new CheckoutResult(false, "Indirizzo spedizione mancante", null, null);
        }

        try {
            Map<String, String> paymentData = Map.of(
                    "card_number", card.number(),
                    "expiry", card.expiry(),
                    "cvv", cvv
            );

            logger.log(Level.FINE, "CVV presente: {0}", !cvv.isBlank() ? "***" : "no");

            PaymentResult payRes = gateway.charge(userId, total, paymentData);
            if (!payRes.success()) {
                return new CheckoutResult(false, "Pagamento rifiutato: " + payRes.message(), payRes.transactionId(), null);
            }

            OrderRepository.CreationResult orderRes = orderRepository.placeOrderWithStockDecrement(
                    userId, items, address);

            logger.log(Level.INFO, "Payment txId: {0}", payRes.transactionId());

            return new CheckoutResult(true, "Pagamento riuscito", payRes.transactionId(), orderRes.orderIds().toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore pagamento", e);
            return new CheckoutResult(false, "Errore durante il pagamento", null, null);
        }
    }

}
