package org.example.controllers.app;

import org.example.controllers.control.PaymentSelectionControl;
import org.example.dao.OrderDAO;
import org.example.gateway.PaymentGateway;
import org.example.gateway.PaymentResult;
import org.example.models.Card;
import org.example.models.CardViewModel;
import org.example.models.CartItem;
import org.example.models.InlineCardData;
import org.example.services.CardsService;
import org.example.services.CardsService.AddCardResult;
import org.example.ui.CardUi;
import org.example.util.Session;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentSelectionAppController implements PaymentSelectionControl {
    private final PaymentGateway gateway;
    private final Logger logger = Logger.getLogger(PaymentSelectionAppController.class.getName());
    private String lastOrderIds;

    public PaymentSelectionAppController(PaymentGateway gateway) {
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

    public CardViewModel addInlineCard(int userId, CardData data) {
        // Converti CardData → InlineCardData (se diverso) o adatta
        InlineCardData inlineData = new InlineCardData(data.holder(), data.number(), data.expiry(), data.type());
        AddCardResult result = CardsService.addInlineCard(userId, inlineData);
        if (result.ok()) {
            return new CardViewModel(result.card());
        }
        logger.log(Level.WARNING, "Aggiunta carta fallita: {0}", result.message());
        return null;
    }


    @Override
    public PaymentResult confirmPayment(Card card, String cvv, String address,
                                        List<CartItem> items, BigDecimal total) {
        if (items == null || total == null) {
            return new PaymentResult(false, "Dati carrello non inizializzati", null, false);
        }
        if (card == null) {
            return new PaymentResult(false, "Carta non selezionata", null, false);
        }
        if (!CardUi.isValidCvv(cvv)) {  // OK utility statica
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

    @Override
    public CardViewModel addInlineCard(InlineCardData data) {
        CardsService.AddCardResult result = CardsService.addInlineCard(Session.getUserId(), data);
        if (result.ok()) {
            return new CardViewModel(result.card());
        }
        logger.log(Level.WARNING, "Add card failed: {0}", result.message());  // Log nel Control
        return null;
    }

    @Override
    public List<CardViewModel> loadSavedCards() {
        try {
            Integer userId = Session.getUserId();
            if(userId == null) return List.of();
            return CardsService.loadSavedCards(userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore caricamento carte", e);
            return List.of();
        }
    }

    @Override
    public String getLastOrderIds() {
        return lastOrderIds;
    }

    public record CardData(String holder, String number, String expiry, String type) {}

}
