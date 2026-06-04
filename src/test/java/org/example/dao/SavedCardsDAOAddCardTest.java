
package org.example.dao;

import org.example.dao.db.DbSavedCardsDAO;
import org.example.database.DatabaseConnection;
import org.example.models.dto.SavedCardData;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SavedCardsDAOAddCardTest {

    private static final int USER_ID = 2; // cliente esistente nel DB originale
    private final SavedCardsRepository savedCardsRepository = new DbSavedCardsDAO();
    private Integer insertedCardId = null; // per cleanup

    @BeforeAll
    void useRealDb() throws Exception {
        // la connessione deve aprirsi senza eccezioni
        DatabaseConnection.getConnection();
    }

    @AfterEach
    void cleanup() throws Exception {
        if (insertedCardId != null) {
            try (var c = DatabaseConnection.getConnection();
                 var ps = c.prepareStatement("DELETE FROM saved_cards WHERE card_id=? AND id_user=?")) {
                ps.setInt(1, insertedCardId);
                ps.setInt(2, USER_ID);
                ps.executeUpdate();
            } finally {
                insertedCardId = null;
            }
        }
    }

    @Test
    @DisplayName("Inserimento carta e rifiuto duplicato (stesse cifre) su DB originale")
    void shouldInsertCardAndAvoidDuplicatesByDigits() {
        // Stato iniziale
        List<SavedCardData> before = savedCardsRepository.findByUser(USER_ID);
        int beforeCount = before.size();

        // Inserisco una nuova carta
        Optional<Integer> maybeId = savedCardsRepository.insertIfAbsentReturningId(
                USER_ID, "Mario Rossi", "4111 1111 1111 1111", "12/27", "Credito");

        assertTrue(maybeId.isPresent(), "L'inserimento della carta dovrebbe restituire un id");
        insertedCardId = maybeId.get();
        assertTrue(insertedCardId > 0, "card_id non valido");

        // Provo un duplicato (stesse cifre, formattazione diversa), deve essere rifiutato
        Optional<Integer> dup = savedCardsRepository.insertIfAbsentReturningId(
                USER_ID, "Mario Rossi", "4111111111111111", "12/27", "Credito");

        assertTrue(dup.isEmpty(), "La carta duplicata (stesse cifre) non dovrebbe essere inserita");

        // Verifico elenco aggiornato: +1 rispetto a prima
        List<SavedCardData> after = savedCardsRepository.findByUser(USER_ID);
        assertEquals(beforeCount + 1, after.size(), "Dovrebbe esserci esattamente una carta in più");
    }
}
