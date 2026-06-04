package org.example.dao.demo;

import org.example.dao.SavedCardsRepository;
import org.example.demo.DemoData;
import org.example.models.dto.Card;
import org.example.models.dto.SavedCardData;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DemoSavedCardsDAO implements SavedCardsRepository {

    @Override
    public List<SavedCardData> findByUser(int userId) {
        DemoData.ensureLoaded();

        var list = DemoData.savedCards().getOrDefault(userId, Collections.emptyList());

        return list.stream()
                .sorted(Comparator.comparingInt(Card::id).reversed())
                .map(card -> new SavedCardData(
                        card.id(),
                        card.holder(),
                        card.number(),
                        card.expiry(),
                        card.type()
                ))
                .toList();
    }

    @Override
    public Optional<Integer> insertIfAbsentReturningId(
            int userId,
            String holder,
            String rawCardNumber,
            String expiry,
            String cardType
    ) {
        DemoData.ensureLoaded();

        String normalized = onlyDigits(rawCardNumber);
        var list = DemoData.savedCards()
                .computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>());

        boolean exists = list.stream()
                .anyMatch(card -> onlyDigits(card.number()).equals(normalized));

        if (exists) {
            return Optional.empty();
        }

        int newId = DemoData.DEMO_CARD_ID.getAndIncrement();
        Card newCard = new Card(newId, holder, rawCardNumber, expiry, cardType);
        list.add(newCard);

        return Optional.of(newId);
    }

    @Override
    public boolean deleteById(int cardId, int userId) {
        DemoData.ensureLoaded();

        var list = DemoData.savedCards().get(userId);
        if (list == null) {
            return false;
        }

        boolean removed = list.removeIf(card -> card.id() == cardId);

        if (list.isEmpty()) {
            DemoData.savedCards().remove(userId);
        }

        return removed;
    }

    @Override
    public boolean updateCard(
            int cardId,
            int userId,
            String holder,
            String rawCardNumber,
            String expiry,
            String cardType
    ) {
        DemoData.ensureLoaded();

        String normalized = onlyDigits(rawCardNumber);
        var list = DemoData.savedCards().get(userId);

        if (list == null) {
            return false;
        }

        boolean duplicate = list.stream()
                .anyMatch(card -> card.id() != cardId && onlyDigits(card.number()).equals(normalized));

        if (duplicate) {
            return false;
        }

        boolean removed = list.removeIf(card -> card.id() == cardId);
        if (!removed) {
            return false;
        }

        Card updatedCard = new Card(cardId, holder, rawCardNumber, expiry, cardType);
        list.add(updatedCard);

        return true;
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}