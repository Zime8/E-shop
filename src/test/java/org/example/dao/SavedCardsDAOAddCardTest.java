/*
 * Responsabile test: Anna Bianchi (matricola 654321)
 * Modulo: SavedCardsDAO – Aggiunta carta cliente (DB originale)
 */
package org.example.dao;

import org.example.database.DatabaseConnection;
import org.example.util.Session;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SavedCardsDAOAddCardTest {

    private static final int USER_ID = 2; // cliente esistente nel DB originale
    private Integer insertedCardId = null; // per cleanup

    @BeforeAll
    void useRealDb() throws Exception {
        // Assicurati di NON usare il demo mode
        Session.setDemo(false);

        // Sanity: la connessione deve aprirsi senza eccezioni
        try (Connection ignored = DatabaseConnection.getInstance()) { /* ok */ }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (insertedCardId != null) {
            try (var c = DatabaseConnection.getInstance();
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
    void shouldInsertCardAndAvoidDuplicatesByDigits() throws Exception {
        // Stato iniziale
        List<SavedCardsDAO.Row> before = SavedCardsDAO.findByUser(USER_ID);
        int beforeCount = before.size();

        // 1) Inserisco una nuova carta
        Optional<Integer> maybeId = SavedCardsDAO.insertIfAbsentReturningId(
                USER_ID, "Mario Rossi", "4111 1111 1111 1111", "12/27", "Credito");

        assertTrue(maybeId.isPresent(), "L'inserimento della carta dovrebbe restituire un id");
        insertedCardId = maybeId.get();
        assertTrue(insertedCardId > 0, "card_id non valido");

        // 2) Provo un duplicato (stesse cifre, formattazione diversa) → deve essere rifiutato
        Optional<Integer> dup = SavedCardsDAO.insertIfAbsentReturningId(
                USER_ID, "Mario Rossi", "4111111111111111", "12/27", "Credito");

        assertTrue(dup.isEmpty(), "La carta duplicata (stesse cifre) non dovrebbe essere inserita");

        // 3) Verifica elenco aggiornato: +1 rispetto a prima
        List<SavedCardsDAO.Row> after = SavedCardsDAO.findByUser(USER_ID);
        assertEquals(beforeCount + 1, after.size(), "Dovrebbe esserci esattamente una carta in più");
    }
}
