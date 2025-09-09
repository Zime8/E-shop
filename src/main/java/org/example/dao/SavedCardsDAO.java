package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.demo.DemoData;
import org.example.models.Card;
import org.example.util.Session;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SavedCardsDAO {

    private SavedCardsDAO(){}

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
    public static List<Row> findByUser(int userId) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var list = DemoData.savedCards().getOrDefault(userId, Collections.emptyList());
            return list.stream()
                    .sorted(Comparator.comparingInt(Card::getId).reversed())
                    .map(c -> new Row(c.getId(), c.getHolder(), c.getNumber(), c.getExpiry(), c.getType()))
                    .toList();
        }

        String call = "{ call sp_cards_find_by_user(?) }";
        return getRows(userId, call);
    }

    private static List<Row> getRows(int userId, String call) throws SQLException {
        List<Row> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, userId);
            try (ResultSet rs = cs.executeQuery()) {
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

    // Inserisce la carta se assente
    public static Optional<Integer> insertIfAbsentReturningId(
            int userId, String holder, String rawCardNumber, String expiry, String cardType) throws SQLException {

        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            final String normalized = onlyDigits(rawCardNumber);
            var list = DemoData.savedCards().computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
            boolean exists = list.stream().anyMatch(c -> onlyDigits(c.getNumber()).equals(normalized));
            if (exists) return Optional.empty();

            int newId = DemoData.DEMO_CARD_ID.getAndIncrement();
            Card c = new Card(newId, holder, rawCardNumber, expiry, cardType);
            list.add(c);
            return Optional.of(newId);
        }

        String call = "{ call sp_cards_insert_if_absent(?, ?, ?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, userId);
            cs.setString(2, holder);
            cs.setString(3, rawCardNumber);
            cs.setString(4, expiry);
            cs.setString(5, cardType);
            cs.registerOutParameter(6, Types.INTEGER);

            cs.execute();

            int id = cs.getInt(6);
            if (cs.wasNull()) return Optional.empty();
            return Optional.of(id);
        }
    }

    public static boolean deleteById(int cardId, int userId) throws SQLException {
        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            var list = DemoData.savedCards().get(userId);
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
            if (list.isEmpty()) DemoData.savedCards().remove(userId);
            return removed;
        }

        String call = "{ call sp_cards_delete(?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, cardId);
            cs.setInt(2, userId);
            cs.registerOutParameter(3, Types.TINYINT);

            cs.execute();
            return cs.getByte(3) == 1;
        }
    }

    public static boolean updateCard(int cardId, int userId, String holder, String rawCardNumber, String expiry, String cardType)
            throws SQLException {

        if (Session.isDemo()) {
            DemoData.ensureLoaded();
            final String normalized = onlyDigits(rawCardNumber);
            var list = DemoData.savedCards().get(userId);
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

        String call = "{ call sp_cards_update(?, ?, ?, ?, ?, ?, ?) }";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(call)) {
            cs.setInt(1, cardId);
            cs.setInt(2, userId);
            cs.setString(3, holder);
            cs.setString(4, rawCardNumber);
            cs.setString(5, expiry);
            cs.setString(6, cardType);
            cs.registerOutParameter(7, Types.TINYINT);

            cs.execute();
            return cs.getByte(7) == 1;
        }
    }
}
