package org.example.dao;

import org.example.database.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopDAO {


    private static final Logger logger = Logger.getLogger(ShopDAO.class.getName());


    public static BigDecimal getBalance(long userId) throws SQLException {

        String sql = """
        SELECT balance
        FROM shops s join users u on s.id_shop= u.id_shop
        WHERE u.id_user=?""";

        try (Connection c = DatabaseConnection.getInstance();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Errore durante il recupero del balance", e);
            return  BigDecimal.ZERO;
        }
    }

    public static void requestWithdraw(long userId, int cardId, BigDecimal amount, String cvv) throws SQLException {
        if (amount == null || amount.signum() <= 0) {
            throw new SQLException("Importo non valido");
        }

        try {
            Thread.sleep(900); //attendiamo 0.9s
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

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
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }


}
