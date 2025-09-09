package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.models.Product;
import org.example.util.ImageUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductCardController {

    private static final Logger LOGGER = Logger.getLogger(ProductCardController.class.getName());

    @FXML private ImageView photo;
    @FXML private Label nameLbl ;
    @FXML private Label priceLbl;
    @FXML private Label nameShopLbl;
    private Product product;
    private Runnable onAddToCartCallback;

    public void setOnAddToCartCallback(Runnable callback) {
        this.onAddToCartCallback = callback;
    }

    public void setProduct(Product p) {

        if(p == null) return;
        this.product = p;

        ImageUtils.setImage(photo, p.getImageData());
        nameLbl.setText(p.getName());
        nameShopLbl.setText(p.getNameShop());
        priceLbl.setText(String.format("€ %.2f", p.getPrice()));
    }

    @FXML
    private void onCardClicked() {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetail.fxml"));
            Parent detailRoot = loader.load();

            // Passa il prodotto al controller del dettaglio prodotto
            ProductDetailController detailCtrl = loader.getController();
            detailCtrl.setProduct(product);

            detailCtrl.setOnAddToCartCallback(onAddToCartCallback);

            // Crea contenitore
            StackPane transparentRoot = new StackPane();
            transparentRoot.setStyle("-fx-background-color: rgba(0, 0, 0, 0.1);");
            transparentRoot.setPadding(new Insets(500));
            transparentRoot.getChildren().add(detailRoot);

            // Aggiungi listener per chiusura cliccando fuori dal contenitore
            transparentRoot.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                if (!detailRoot.isHover()) {
                    Stage stage = (Stage) transparentRoot.getScene().getWindow();
                    stage.close();
                }
            });

            // Semitrasparenza intorno al contenitore
            Stage owner = (Stage) photo.getScene().getWindow();
            Scene scene = new Scene(transparentRoot, owner.getWidth(), owner.getHeight());
            scene.setFill(Color.TRANSPARENT);

            owner.widthProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setWidth(newVal.doubleValue())
            );

            owner.heightProperty().addListener((obs, oldVal, newVal) ->
                scene.getWindow().setHeight(newVal.doubleValue())
            );

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initOwner(photo.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);

            dialog.setScene(scene);

            dialog.showAndWait();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossibile caricare ProductDetail.fxml", e);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Errore durante visualizzazione dettaglio", ex);
        }
    }

}
