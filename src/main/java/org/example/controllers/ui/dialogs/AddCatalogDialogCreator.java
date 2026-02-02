package org.example.controllers.ui.dialogs;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.example.controllers.app.SellerHomeAppController;
import org.example.models.CatalogForm;

import java.math.BigDecimal;

public class AddCatalogDialogCreator extends CatalogDialogCreator {
    public AddCatalogDialogCreator(Button ownerButton, SellerHomeAppController appController) {
        super("Aggiungi Prodotto", null, ownerButton, appController);
    }

    @Override
    protected void attachValidationAndResult(Dialog<CatalogForm> dialog, ProductUI ui, Styles styles, CatalogForm initial) {
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            resetStyles(ui, styles, initial);
            if (!validateUIForm(ui, initial, styles)) {
                ev.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return createValidatedForm(ui, initial);
        });
    }

    private void resetStyles(ProductUI ui, Styles styles, CatalogForm initial) {
        if (initial == null) ui.combo().getEditor().setStyle(styles.base());
        ui.size().setStyle(styles.base());
        ui.price().setStyle(styles.base());
        ui.qty().setStyle(styles.base());
    }

    private boolean validateUIForm(ProductUI ui, CatalogForm initial, Styles styles) {
        if (initial == null && ui.combo().getValue() == null) {
            ui.combo().getEditor().setStyle(styles.error());
            throw new IllegalArgumentException("Seleziona un prodotto cliccando sulla lista");
        }
        if (!ui.size().isDisabled() && ui.size().getText().isBlank()) {
            ui.size().setStyle(styles.error());
            throw new IllegalArgumentException("Taglia obbligatoria");
        }
        try {
            new BigDecimal(ui.price().getText().trim());
            Integer.parseInt(ui.qty().getText().trim());
            return true;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage());  // Ri-lancia per semplicità
        }
    }

    private CatalogForm createValidatedForm(ProductUI ui, CatalogForm initial) {
        try {
            int pid = initial != null ? initial.productId() : ui.combo().getValue().productId();
            String size = ui.size().getText().trim();
            BigDecimal price = new BigDecimal(ui.price().getText().trim());
            int qty = Integer.parseInt(ui.qty().getText().trim());

            CatalogForm form = new CatalogForm(pid, size, price, qty);

            if (appController.isValidCatalogForm(form)) {
                return form;
            } else {
                showAlert(Alert.AlertType.WARNING, "Prezzo o quantità non validi");
                return null;
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.WARNING, "Errore form: " + ex.getMessage());
            return null;
        }
    }

}