package org.example.controllers.ui.dialogs;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.example.controllers.app.SellerHomeAppController;
import org.example.models.CatalogForm;

import java.math.BigDecimal;

public class EditCatalogDialogCreator extends CatalogDialogCreator {
    public EditCatalogDialogCreator(CatalogForm initial, Button ownerButton, SellerHomeAppController appController) {
        super("Modifica Prodotto", initial, ownerButton, appController);
    }

    @Override
    protected void attachValidationAndResult(Dialog<CatalogForm> dialog, ProductUI ui, Styles styles, CatalogForm initial) {
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                // Reset styles
                ui.size().setStyle(styles.base());
                ui.price().setStyle(styles.base());
                ui.qty().setStyle(styles.base());

                // Validazioni
                if (ui.size().getText().isBlank()) {
                    ui.size().setStyle(styles.error());
                    throw new IllegalArgumentException("Taglia obbligatoria");
                }
                new BigDecimal(ui.price().getText().trim());
                Integer.parseInt(ui.qty().getText().trim());

            } catch (Exception ex) {
                ev.consume();
                showAlert(Alert.AlertType.WARNING, "Dati non validi: " + ex.getMessage());
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return extractForm(ui, initial.productId());
        });
    }

}
