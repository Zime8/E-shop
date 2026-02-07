package org.example.controllers.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.controllers.app.ProductCardAppController;
import org.example.controllers.app.ProductDetailAppController;
import org.example.models.Product;
import org.example.util.ImageUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductCardController {

    private static final Logger logger = Logger.getLogger(ProductCardController.class.getName());

    @FXML private ImageView photo;
    @FXML private Label nameLbl ;
    @FXML private Label priceLbl;
    @FXML private Label nameShopLbl;

    private ProductCardAppController appController;

    public void setController(ProductCardAppController appController) {
        this.appController = appController;
    }

    public void setProduct(Product p) {
        if(p == null) return;

        ImageUtils.setImage(photo, p.getImageData());
        nameLbl.setText(p.getName());
        nameShopLbl.setText(p.getNameShop());
        priceLbl.setText(String.format("€ %.2f", p.getPrice()));

        appController.setProduct(p, appController.getOnAddToCartCallback());
    }

    public void setOnCartUpdate(Runnable callback) {
        if (appController != null) {
            appController.setOnAddToCartCallback(callback);
        }
    }

    @FXML private void onCardClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetail.fxml"));
            Parent detailRoot = loader.load();

            ProductDetailController detailCtrl = loader.getController();
            detailCtrl.setAppController(new ProductDetailAppController());
            detailCtrl.setProduct(appController.getCurrentProduct());
            detailCtrl.setOnCartUpdate(appController.getOnAddToCartCallback());

            Pane transparentRoot = new Pane();
            transparentRoot.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
            transparentRoot.getChildren().add(detailRoot);

            detailRoot.setLayoutX(0);
            detailRoot.setLayoutY(0);
            detailRoot.autosize();

            transparentRoot.layoutBoundsProperty().addListener((obs, old, neu) -> {
                detailRoot.setLayoutX((transparentRoot.getWidth() - detailRoot.getBoundsInLocal().getWidth()) / 2);
                detailRoot.setLayoutY((transparentRoot.getHeight() - detailRoot.getBoundsInLocal().getHeight()) / 2);
            });

            transparentRoot.setOnMouseClicked(e -> {
                if (!detailRoot.getBoundsInParent().contains(e.getX(), e.getY())) {
                    ((Stage)transparentRoot.getScene().getWindow()).close();
                }
            });

            Scene scene = new Scene(transparentRoot);
            scene.setFill(Color.TRANSPARENT);

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            Stage primaryStage = (Stage) photo.getScene().getWindow();
            dialog.initOwner(photo.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);

            dialog.setX(primaryStage.getX());
            dialog.setY(primaryStage.getY());
            dialog.setWidth(primaryStage.getWidth());
            dialog.setHeight(primaryStage.getHeight());

            dialog.showAndWait();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossibile caricare ProductDetail.fxml", e);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Errore durante visualizzazione dettaglio", ex);
        }
    }
}
