package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.Card;
import org.example.util.Session;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SavedCardsDAO {

    public record Row(
            int id,
            String holder,
            String cardNumber,
            String expiry,
            String cardType
    ) {
        public int getId() { return id; }
        public String getHolder() { return holder; }
        public String getCardNumber() { return cardNumber; }
        public String getExpiry() { return expiry; }
        public String getCardType() { return cardType; }
    }

    private static String onlyDigits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    // Legge tutte le carte dell’utente
    public List<Row> findByUser(int userId) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var list = DemoData.SAVED_CARDS.getOrDefault(userId, Collections.emptyList());
            // Ordina per id DESC (proxy di created_at)
            return list.stream()
                    .sorted(Comparator.comparingInt(Card::getId).reversed())
                    .map(c -> new Row(c.getId(), c.getHolder(), c.getNumber(), c.getExpiry(), c.getType()))
                    .collect(Collectors.toList());
        }

        String sql = """
            SELECT card_id, holder, card_number, expiry, card_type
            FROM saved_cards
            WHERE id_user = ?
            ORDER BY created_at DESC, card_id DESC
        """;
        List<Row> out = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Row(
                            rs.getInt("card_id"),
                            rs.getString("holder"),
                            rs.getString("card_number"),
                            rs.getString("expiry"),
                            rs.getString("card_type")
                    ));
                }
            }
        }
        return out;
    }

    // Inserisce la carta se assente (ritorna id)
    public Optional<Integer> insertIfAbsentReturningId(
            int userId, String holder, String rawCardNumber, String expiry, String cardType) throws SQLException {

        final String normalized = onlyDigits(rawCardNumber);

        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var list = DemoData.SAVED_CARDS.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
            boolean exists = list.stream().anyMatch(c -> onlyDigits(c.getNumber()).equals(normalized));
            if (exists) return Optional.empty();

            int newId = DemoData.DEMO_CARD_ID.getAndIncrement();
            Card c = new Card(newId, holder, rawCardNumber, expiry, cardType);
            list.add(c);
            return Optional.of(newId);
        }

        final String sql = """
            INSERT INTO saved_cards (id_user, holder, card_number, expiry, card_type)
            SELECT ?, ?, ?, ?, ?
            FROM DUAL
            WHERE NOT EXISTS (
                SELECT 1
                FROM saved_cards
                WHERE id_user = ?
                  AND REGEXP_REPLACE(card_number, '[^0-9]', '') = ?
            )
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, holder);
            ps.setString(3, rawCardNumber);
            ps.setString(4, expiry);
            ps.setString(5, cardType);
            ps.setInt(6, userId);
            ps.setString(7, normalized);

            int affected = ps.executeUpdate();
            if (affected == 0) return Optional.empty(); // già presente

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? Optional.of(keys.getInt(1)) : Optional.empty();
            }
        }
    }

    public boolean deleteById(int cardId, int userId) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var list = DemoData.SAVED_CARDS.get(userId);
            if (list == null) return false;

            boolean removed = false;
            for (var it = list.iterator(); it.hasNext(); ) {
                Card c = it.next();
                if (c.getId() == cardId) {
                    it.remove();
                    removed = true;
                    break;
                }
            }
            if (list.isEmpty()) DemoData.SAVED_CARDS.remove(userId);
            return removed;
        }

        String sql = """
            DELETE FROM saved_cards
            WHERE card_id = ? AND id_user = ?
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, cardId);
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }

    public boolean updateCard(int cardId, int userId, String holder, String rawCardNumber, String expiry, String cardType)
            throws SQLException {

        final String normalized = onlyDigits(rawCardNumber);

        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var list = DemoData.SAVED_CARDS.get(userId);
            if (list == null) return false;

            boolean dup = list.stream().anyMatch(c ->
                    c.getId() != cardId && onlyDigits(c.getNumber()).equals(normalized));
            if (dup) return false;

            for (Card c : list) {
                if (c.getId() == cardId) {
                    c.setHolder(holder);
                    c.setNumber(rawCardNumber);
                    c.setExpiry(expiry);
                    c.setType(cardType);
                    return true;
                }
            }
            return false;
        }

        String sql = """
            UPDATE saved_cards s
            SET s.holder = ?, s.card_number = ?, s.expiry = ?, s.card_type = ?
            WHERE s.card_id = ? AND s.id_user = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM (
                      SELECT card_id
                      FROM saved_cards
                      WHERE id_user = ? AND REGEXP_REPLACE(card_number, '[^0-9]', '') = ?
                  ) AS x
                  WHERE x.card_id <> s.card_id
              )
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, holder);
            ps.setString(2, rawCardNumber);
            ps.setString(3, expiry);
            ps.setString(4, cardType);
            ps.setInt(5, cardId);
            ps.setInt(6, userId);
            ps.setInt(7, userId);
            ps.setString(8, normalized);

            int affected = ps.executeUpdate();
            return affected > 0;
        }
    }
}
