package org.example.boundary.dialogs;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.example.control.SellerProductsController;
import org.example.models.dto.CatalogForm;

public class EditCatalogDialogCreator extends CatalogDialogCreator {
    public EditCatalogDialogCreator(CatalogForm initial, Button ownerButton,
                                    SellerProductsController productsController) {
        super("Modifica Prodotto", initial, ownerButton, productsController);
    }

    @Override
    protected void attachValidationAndResult(Dialog<CatalogForm> dialog, ProductUI ui, Styles styles, CatalogForm initial) {
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            resetCommonFieldStyles(ui, styles);
            if (!validateCommonFields(ui, styles)) {
                ev.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return extractForm(ui, initial.productId());
        });
    }

}
