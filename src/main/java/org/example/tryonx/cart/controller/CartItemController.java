package org.example.tryonx.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.cart.dto.DeleteRequest;
import org.example.tryonx.cart.dto.PutInCartRequestDto;
import org.example.tryonx.cart.dto.UpdateCartItemRequestDto;
import org.example.tryonx.cart.service.CartItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "User Carts API", description = "회원 장바구니 API")
public class CartItemController {
    private final CartItemService cartItemService;
    public CartItemController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @PostMapping
    @Operation(summary = "장바구니 추가")
    public ResponseEntity<?> addCartItem(@AuthenticationPrincipal UserDetails userDetails, @RequestBody PutInCartRequestDto dto) {
        String email = userDetails.getUsername();
        cartItemService.addCartItem(email, dto);
        return ResponseEntity.ok("Added cart item");
    }

//    @GetMapping
//    public ResponseEntity<?> getCartItems(@AuthenticationPrincipal UserDetails userDetails) {
//        String email = userDetails.getUsername();
//        return ResponseEntity.ok(cartItemService.getCartItems(email));
//    }

    @DeleteMapping
    @Operation(summary = "장바구니 삭제")
    public ResponseEntity<?> deleteCartItem(@AuthenticationPrincipal UserDetails userDetails, @RequestBody List<DeleteRequest> deleteRequests) {
        String email = userDetails.getUsername();
        cartItemService.deleteCartItem(email, deleteRequests);
        return ResponseEntity.ok("Deleted cart item");
    }

    @PatchMapping
    @Operation(summary = "옵션 변경")
    public ResponseEntity<?> updateCartItem(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UpdateCartItemRequestDto updateCartItemRequestDto) {
        String email = userDetails.getUsername();
        cartItemService.updateCartItem(email, updateCartItemRequestDto);
        return ResponseEntity.ok("Updated cart item");
    }

    @GetMapping
    @Operation(summary = "장바구니 조회")
    public ResponseEntity<?> getCartItems(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(cartItemService.getCartItems(email));
    }

//    @GetMapping("/selected")
//    public ResponseEntity<?> getSelectedCartItems(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam(required = false) List<Long> ids) {
//        String email = userDetails.getUsername();
//        return ResponseEntity.ok(cartItemService.getCartWithCheckedInfo(email, ids));
//    }
}
