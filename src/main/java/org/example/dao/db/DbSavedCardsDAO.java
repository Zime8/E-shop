package org.example.dao.db;

import org.example.dao.RepositoryException;
import org.example.dao.SavedCardsRepository;
import org.example.database.DatabaseConnection;
import org.example.models.dto.SavedCardData;

import java.sql.*;
import java.util.*;

public final class DbSavedCardsDAO implements SavedCardsRepository {

    // Legge tutte le carte dell’utente
    @Override
    public List<SavedCardData> findByUser(int userId) {
        String call = "{ call sp_cards_find_by_user(?) }";

        try {
            return getRows(userId, call);
        } catch (SQLException e) {
            throw new RepositoryException("Errore durante il recupero delle carte salvate per userId=" + userId, e);
        }
    }

    private static List<SavedCardData> getRows(int userId, String call) throws SQLException{
        List<SavedCardData> rows = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new SavedCardData(
                            resultSet.getInt("card_id"),
                            resultSet.getString("holder"),
                            resultSet.getString("card_number"),
                            resultSet.getString("expiry"),
                            resultSet.getString("card_type")
                    ));
                }
            }
        }
        return rows;
    }

    // Inserisce la carta se assente
    @Override
    public Optional<Integer> insertIfAbsentReturningId(
            int userId, String holder, String rawCardNumber, String expiry, String cardType) {

        String call = "{ call sp_cards_insert_if_absent(?, ?, ?, ?, ?, ?) }";

        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setInt(1, userId);
            statement.setString(2, holder);
            statement.setString(3, rawCardNumber);
            statement.setString(4, expiry);
            statement.setString(5, cardType);
            statement.registerOutParameter(6, Types.INTEGER);

            statement.execute();

            int id = statement.getInt(6);
            if (statement.wasNull()) return Optional.empty();
            return Optional.of(id);
        } catch (SQLException e) {
            throw new RepositoryException("Errore durante l'inserimento della carta per userId=" + userId, e);
        }
    }

    @Override
    public boolean deleteById(int cardId, int userId) {

        String call = "{ call sp_cards_delete(?, ?, ?) }";

        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setInt(1, cardId);
            statement.setInt(2, userId);
            statement.registerOutParameter(3, Types.TINYINT);

            statement.execute();
            return statement.getByte(3) == 1;
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Errore durante l'eliminazione della carta id=" + cardId + " per userId=" + userId,
                    e);
        }
    }

    @Override
    public boolean updateCard(int cardId, int userId, String holder, String rawCardNumber, String expiry, String cardType) {

        String call = "{ call sp_cards_update(?, ?, ?, ?, ?, ?, ?) }";

        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setInt(1, cardId);
            statement.setInt(2, userId);
            statement.setString(3, holder);
            statement.setString(4, rawCardNumber);
            statement.setString(5, expiry);
            statement.setString(6, cardType);
            statement.registerOutParameter(7, Types.TINYINT);

            statement.execute();
            return statement.getByte(7) == 1;
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Errore durante l'aggiornamento della carta id=" + cardId + " per userId=" + userId,
                    e);
        }
    }
}
