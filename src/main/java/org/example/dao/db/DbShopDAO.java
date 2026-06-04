package org.example.dao.db;

import org.example.dao.RepositoryException;
import org.example.dao.ShopRepository;
import org.example.database.DatabaseConnection;
import org.example.models.dto.SellerShop;
import org.example.models.entity.Shop;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DbShopDAO implements ShopRepository {

    private static final Logger logger = Logger.getLogger(DbShopDAO.class.getName());

    public DbShopDAO(){}

    // Saldo del venditore
    @Override
    public BigDecimal getBalance(long userId){
        final String call = "{ call sp_shop_get_balance_by_user(?) }";
        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, e, () ->
                    "Errore durante il recupero del balance per userId=" + userId);
            throw new RepositoryException("Errore nel recupero del balance per userId=" + userId, e);
        }
    }

    @Override
    public void requestWithdraw(long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Importo non valido");
        }

        final String call = "{ call sp_shop_request_withdraw(?, ?) }";
        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setLong(1, userId);
            statement.setBigDecimal(2, amount);
            statement.execute();
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Errore nella richiesta di prelievo (userId=" + userId + ", amount=" + amount + ")", e
            );
        }
    }

    // Restituisce il negozio con via e telefono
    @Override
    public Shop getById(long idShop) {
        final String call = "{ call sp_shop_get_by_id(?) }";
        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement statement = connection.prepareCall(call)) {
            statement.setLong(1, idShop);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Shop(
                            resultSet.getLong("id_shop"),
                            resultSet.getString("name_s"),
                            resultSet.getString("street"),
                            resultSet.getString("phone_number")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, e, () ->
                    "Errore durante il recupero del negozio idShop=" + idShop);
            throw new RepositoryException("Errore durante il recupero del negozio idShop=" + idShop, e);
        }
    }

    @Override
    public Optional<SellerShop> findShopForUser(long userId) {
        final String call = "{ call sp_seller_find_shop(?) }";
        try (Connection c = DatabaseConnection.getConnection();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, userId);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SellerShop(
                            rs.getInt("id_shop"),
                            rs.getString("name_s")
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, e, () ->
                    "Errore durante il recupero del negozio per userId=" + userId);
            throw new RepositoryException(
                    "Errore durante il recupero del negozio per userId=" + userId, e);
        }
    }
}
