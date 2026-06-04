package org.example.control.dependencies;

import org.example.control.CheckOrders;
import org.example.control.ModifyProfile;
import org.example.control.WishlistAppController;
import org.example.control.services.*;
import org.example.control.session.UserContext;

public record BuyProductDependencies(HomeService homeService,
                                     CartService cartService,
                                     ProductDetailService productDetailService,
                                     PaymentSelectionService paymentService,
                                     UserContext userContext,
                                     ModifyProfile modifyProfileController,
                                     CheckOrders checkOrdersController,
                                     WishlistAppController wishlistAppController) {
}
