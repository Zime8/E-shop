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

    // Saldo del venditore
    public static BigDecimal getBalance(long userId) throws SQLException {
        final String call = "{ call sp_shop_get_balance_by_user(?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, userId);
            try (ResultSet rs = cs.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, e, () ->
                    "Errore durante il recupero del balance per userId=" + userId);
            return null;
        }
    }

    public static void requestWithdraw(long userId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.signum() <= 0) {
            throw new SQLException("Importo non valido");
        }

        // piccolo delay
        try { Thread.sleep(900); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        final String call = "{ call sp_shop_request_withdraw(?, ?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, userId);
            cs.setBigDecimal(2, amount);
            cs.execute(); // la SP fa transazione + SIGNAL su errore
        } catch (SQLException e) {
            logger.log(Level.WARNING, e, () ->
                    "Errore nella richiesta di prelievo: userId=" + userId + ", amount=" + amount);
            throw e; // Propaga per gestirlo a livello superiore (coerente con firma throws)
        }
    }

    // Restituisce il negozio con via e telefono; null se non trovato
    public static Shop getById(long idShop) {
        final String call = "{ call sp_shop_get_by_id(?) }";
        try (Connection c = DatabaseConnection.getInstance();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, idShop);
            try (ResultSet rs = cs.executeQuery()) {
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
            return null; // mantieni comportamento precedente
        }
    }
}
