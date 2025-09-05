package org.example.ui;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.models.Card;

import java.util.Map;

/** Cella riutilizzabile con TextField CVV (solo cifre, max 3). */
public class CvvTableCell extends TableCell<Card, String> {
    private final TextField tf = new TextField();
    private final TableView<Card> table;
    private final Map<Integer, String> transientCvvs;

    public CvvTableCell(TableView<Card> table, Map<Integer, String> transientCvvs) {
        this.table = table;
        this.transientCvvs = transientCvvs;

        tf.setPromptText("CVV");
        tf.setPrefWidth(70);

        // solo cifre, max 3
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            String digits = newV.replaceAll(CardUi.DIGITS_ONLY_REGEX, "");
            if (!digits.equals(newV)) { tf.setText(digits); return; }
            if (digits.length() > 3) tf.setText(digits.substring(0, 3));
        });

        // memorizza CVV temporaneo per la riga corrente
        tf.textProperty().addListener((obs, oldV, newV) -> {
            Card row = currentRow();
            if (row == null) return;
            if (newV == null || newV.isEmpty()) transientCvvs.remove(row.getId());
            else transientCvvs.put(row.getId(), newV);
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) { setGraphic(null); return; }
        Card row = currentRow();
        if (row == null) { setGraphic(null); return; }
        tf.setText(transientCvvs.getOrDefault(row.getId(), ""));
        boolean selected = table.getSelectionModel().getSelectedItem() == row;
        tf.setDisable(!selected);
        setGraphic(tf);
    }

    private Card currentRow() {
        return (getTableRow() == null) ? null : getTableRow().getItem();
    }
}
