package org.example.dao;

import org.example.models.dto.SavedCardData;

import java.util.List;
import java.util.Optional;

public interface SavedCardsRepository {

    List<SavedCardData> findByUser(int userId);

    Optional<Integer> insertIfAbsentReturningId(
            int userId,
            String holder,
            String rawCardNumber,
            String expiry,
            String cardType
    );

    boolean deleteById(int cardId, int userId);

    boolean updateCard(
            int cardId,
            int userId,
            String holder,
            String rawCardNumber,
            String expiry,
            String cardType
    );
}