package org.example.boundary.dialogs;

import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.example.control.SellerProductsController;
import org.example.models.dto.CatalogForm;

import java.math.BigDecimal;

public class AddCatalogDialogCreator extends CatalogDialogCreator {
    public AddCatalogDialogCreator(Button ownerButton,
                                   SellerProductsController productsController) {
        super("Aggiungi Prodotto", null, ownerButton, productsController);
    }

    @Override
    protected void attachValidationAndResult(Dialog<CatalogForm> dialog, ProductUI ui, Styles styles, CatalogForm initial) {
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            resetStyles(ui, styles);
            if (!validateUIForm(ui, styles)) {
                ev.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return createValidatedForm(ui);
        });
    }

    private void resetStyles(ProductUI ui, Styles styles) {
        ui.combo().getEditor().setStyle(styles.base());
        resetCommonFieldStyles(ui, styles);
    }

    private boolean validateUIForm(ProductUI ui, Styles styles) {
        if (ui.combo().getValue() == null) {
            ui.combo().getEditor().setStyle(styles.error());
            showAlert(Alert.AlertType.WARNING, "Seleziona un prodotto cliccando sulla lista");
            return false;
        }

        return validateCommonFields(ui, styles);
    }

    private CatalogForm createValidatedForm(ProductUI ui) {
        try {
            int pid = ui.combo().getValue().productId();
            String size = ui.size().getText().trim();
            BigDecimal price = new BigDecimal(ui.price().getText().trim());
            int qty = Integer.parseInt(ui.qty().getText().trim());

            CatalogForm form = new CatalogForm(pid, size, price, qty);

            if (productsController.isValidCatalogForm(form)) {
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