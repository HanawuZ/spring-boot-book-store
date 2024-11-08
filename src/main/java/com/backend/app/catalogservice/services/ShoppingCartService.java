package com.backend.app.catalogservice.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.backend.app.catalogservice.repositories.CartRepository;
import com.backend.app.catalogservice.models.ShoppingCartModel.CartQueryResult;
import com.backend.app.catalogservice.models.ShoppingCartModel.ShoppingCartResponse;
import com.backend.app.catalogservice.models.ShoppingCartModel.AddItemRequest;
import com.backend.app.catalogservice.models.ShoppingCartModel.CartItem;
import com.backend.app.shared.libraries.http.BaseResponse;
import com.backend.app.shared.models.entities.Book;
import com.backend.app.shared.models.entities.Cart;


interface IShoppingCartService {
    
    BaseResponse<ShoppingCartResponse> getCartItemsByUserId(String userId);
    
    BaseResponse<?> addItemToCart(AddItemRequest request);
    
    BaseResponse<?> deleteCartItem(String customerId, String bookId);
    
}

@Service
public class ShoppingCartService implements IShoppingCartService {

    private CartRepository cartRepository;

    public ShoppingCartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public BaseResponse<ShoppingCartResponse> getCartItemsByUserId(String userId) {
        try {
            List<CartQueryResult> results = cartRepository.getCartItemsByUserId(userId);
            if (results.isEmpty()) {
                return new BaseResponse<>(4000, "Cart is empty", null);
            }

            ShoppingCartResponse shoppingCart = new ShoppingCartResponse();

            List<CartItem> cartItems = new ArrayList<>();
            Double totalPrice = 0.0;

            for (CartQueryResult result : results) {
                totalPrice += result.getPrice() * result.getQuantity();
                CartItem item = new CartItem();
                item.setBookId(result.getBookId());
                item.setTitle(result.getTitle());
                item.setGenre(result.getGenre());
                item.setPrice(result.getPrice());
                item.setQuantity(result.getQuantity());

                cartItems.add(item);
            }

            shoppingCart.setTotalPrice(totalPrice);
            shoppingCart.setItems(cartItems);

            return new BaseResponse<>(2000, "Cart items retrieved successfully", shoppingCart);
        } catch (Exception e) {
            e.printStackTrace();
            String error = String.format("Internal server error: %s", e.getMessage());
            return new BaseResponse<>(5000, error, null);
        }
    }

    public BaseResponse<?> addItemToCart(AddItemRequest request) {
        try {

            List<Cart> carts = cartRepository.getCartByCustomerId(request.getCustomerId());
            
            for (Integer i = 0; i < carts.size(); i++) {
                Book book = carts.get(i).getBook();
                if (book.getId().equals(request.getBookId())) {
                    request.setQuantity(request.getQuantity());
                    break;
                }
            }

            Boolean completed = cartRepository.upsertCartItem(request);
            if (completed.equals(false)) {
                return new BaseResponse<>(4000, "Failed to update item to cart", null);
            }

            return new BaseResponse<>(2000, "Successfully update items to cart", null);
        } catch (Exception e) {
            e.printStackTrace();
            String error = String.format("Internal server error: %s", e.getMessage());
            return new BaseResponse<>(5000, error, null);
        }
    }

    
    public BaseResponse<?> deleteCartItem(String customerId, String bookId) {
        try {
            Boolean completed = cartRepository.deleteItem(customerId, bookId);

            if (completed.equals(false)) {
                return new BaseResponse<>(4000, "Failed to delete item from cart", null);
            }

            return new BaseResponse<>(2000, "Item deleted from cart successfully", null);
        } catch (Exception e) {
            e.printStackTrace();
            String error = String.format("Internal server error: %s", e.getMessage());
            return new BaseResponse<>(5000, error, null);
        }
    }
}
