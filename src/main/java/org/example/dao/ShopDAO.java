package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.models.Shop;

import java.math.BigDecimal;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ShopDAO {

    private ShopDAO() {}

    private static final Logger logger = Logger.getLogger(ShopDAO.class.getName());

    public static BigDecimal getBalance(long userId) throws SQLException {
        final String sql = """
            SELECT balance
            FROM shops s JOIN users u ON s.id_shop = u.id_shop
            WHERE u.id_user = ?
        """;

        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            // Log nel DAO
            logger.log(Level.WARNING, e, () ->
                    "Errore durante il recupero del balance per userId=" + userId);
            return null;
        }
    }

    public static void requestWithdraw(long userId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.signum() <= 0) {
            throw new SQLException("Importo non valido");
        }

        try { Thread.sleep(900); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        final String SQL_UPDATE_BALANCE = """
            UPDATE shops s
            JOIN users u ON u.id_shop = s.id_shop
            SET s.balance = s.balance - ?
            WHERE u.id_user = ? AND s.balance >= ?
        """;

        try (Connection c = DatabaseConnection.getInstance()) {
            c.setAutoCommit(false);
            try (PreparedStatement psUpd = c.prepareStatement(SQL_UPDATE_BALANCE)) {

                psUpd.setBigDecimal(1, amount);
                psUpd.setLong(2, userId);
                psUpd.setBigDecimal(3, amount);

                int changed = psUpd.executeUpdate();
                if (changed == 0) {
                    c.rollback();
                    throw new SQLException("Saldo insufficiente o shop non trovato");
                }

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                // Log nel DAO
                logger.log(Level.WARNING, e, () ->
                        "Errore nella richiesta di prelievo: userId=" + userId + ", amount=" + amount);
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Restituisce il negozio con via e telefono; null se non trovato. */
    public static Shop getById(long idShop) throws SQLException {
        final String sql = """
            SELECT id_shop, name_s, street, phone_number
            FROM shops
            WHERE id_shop = ?
        """;

        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idShop);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Shop(
                            rs.getLong("id_shop"),
                            rs.getString("name_s"),
                            rs.getString("street"),
                            rs.getString("phone_number")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, e, () ->
                    "Errore durante il recupero del negozio idShop=" + idShop);
            return null;
        }
    }
}
