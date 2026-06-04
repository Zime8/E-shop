package org.example.config;

import javafx.util.Callback;
import org.example.boundary.*;

import java.lang.reflect.InvocationTargetException;

public class FxControllerFactory implements Callback<Class<?>, Object> {

    private final AppContext appContext;

    public FxControllerFactory(AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public Object call(Class<?> type) {

        if (type == HomeController.class) {
            HomeController controller = new HomeController();
            controller.setAppController(appContext.getBuyProductController());
            controller.setReviewAppController(appContext.getReviewAppController());
            controller.setNavigator(appContext.getNavigator());
            return controller;
        }

        if (type == SellerHomeController.class) {
            SellerHomeController controller = new SellerHomeController();
            controller.setAppController(appContext.getSellerHomeAppController());
            controller.setOrdersController(appContext.getSellerOrdersController());
            controller.setProductsController(appContext.getSellerProductsController());
            controller.setNavigator(appContext.getNavigator());
            return controller;
        }

        if (type == ProductDetailController.class) {
            ProductDetailController controller = new ProductDetailController();
            controller.setAppController(appContext.getBuyProductController());
            controller.setWishlistController(appContext.getWishlistAppController());
            controller.setReviewAppController(appContext.getReviewAppController());
            controller.setNavigator(appContext.getNavigator());
            return controller;
        }

        if (type == ReviewController.class) {
            ReviewController controller = new ReviewController();
            controller.setAppController(appContext.getReviewAppController());
            controller.setNavigator(appContext.getNavigator());
            return controller;
        }

        if (type == WithdrawSelectionController.class) {
            WithdrawSelectionController controller = new WithdrawSelectionController();
            controller.setAppController(appContext.getWithdrawController());
            return controller;
        }

        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new ControllerCreationException(
                    "Impossibile creare il controller: " + type.getName(), e);
        }
    }
}
