package org.example.tryonx.cart.service;

import jakarta.transaction.Transactional;
import org.example.tryonx.cart.domain.CartItem;
import org.example.tryonx.cart.dto.*;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.enums.Size;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartItemService {
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;

    public CartItemService(CartItemRepository cartItemRepository, OrderRepository orderRepository, MemberRepository memberRepository, ProductRepository productRepository, ProductItemRepository productItemRepository, ProductImageRepository productImageRepository) {
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.productItemRepository = productItemRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional
    public void addCartItem(String email, PutInCartRequestDto requestDto){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 사용자가 없습니다."));
        requestDto.getItems().forEach(item -> {
            ProductItem productItem = productItemRepository.findByProductAndSize(productRepository.findById(item.getProductId()).get(), item.getSize())
                    .orElse(null);

            if(productItem == null){
                throw new RuntimeException("해당 사이즈 혹은 상품이 없습니다.");
            }

            CartItem cartItem = cartItemRepository.findByMemberAndProductItem(member, productItem)
                    .orElse(null);

            if(cartItem != null){
                if(item.getQuantity().equals(cartItem.getQuantity())){
                    throw new RuntimeException("장바구니에 같은 상품이 존재합니다.");
                }
                cartItem.setQuantity(item.getQuantity());
                cartItemRepository.save(cartItem);
                return;
            }

            CartItem newItem = CartItem.builder()
                    .member(member)
                    .productItem(productItem)
                    .quantity(item.getQuantity())
                    .addedAt(LocalDateTime.now())
                    .build();
            cartItemRepository.save(newItem);
        });
    }

    @Transactional
    public CartListResponseDto getCartItems(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 사용자가 없습니다."));
        List<CartItem> cartItems = cartItemRepository.findByMember(member);
        List<Item> items = cartItems.stream().map(this::toItemDto).toList();

        // 총 상품 가격 계산
        BigDecimal productPrice = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = BigDecimal.ZERO;
        BigDecimal totalPrice = productPrice.add(deliveryFee);

        int expectedPoint = productPrice.multiply(BigDecimal.valueOf(0.01))
                .intValue(); // 1% 적립

        return CartListResponseDto.builder()
                .productPrice(productPrice)
                .deliveryFee(deliveryFee)
                .totalPrice(totalPrice)
                .expectedPoint(expectedPoint)
                .items(items)
                .build();
    }

    public CartListResponseDto getCartWithCheckedInfo(String email, List<Long> checkedIds) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        List<CartItem> allCartItems = cartItemRepository.findByMember(member);
        List<Item> items = allCartItems.stream().map(this::toItemDto).toList();

        // checkedIds가 null이거나 빈 경우 → 금액 정보는 0 처리
        if (checkedIds == null || checkedIds.isEmpty()) {
            return CartListResponseDto.builder()
                    .productPrice(BigDecimal.ZERO)
                    .deliveryFee(BigDecimal.ZERO)
                    .totalPrice(BigDecimal.ZERO)
                    .expectedPoint(0)
                    .items(items)
                    .build();
        }

        // 체크된 항목만 필터링
        List<CartItem> checkedItems = allCartItems.stream()
                .filter(item -> checkedIds.contains(item.getCartItemId()))
                .toList();

        BigDecimal productPrice = checkedItems.stream()
                .map(i -> i.getProductItem().getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = productPrice.compareTo(BigDecimal.valueOf(50000)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(3000);
        BigDecimal totalPrice = productPrice.add(deliveryFee);
        int expectedPoint = productPrice.multiply(BigDecimal.valueOf(0.01)).intValue();

        return CartListResponseDto.builder()
                .productPrice(productPrice)
                .deliveryFee(deliveryFee)
                .totalPrice(totalPrice)
                .expectedPoint(expectedPoint)
                .items(items)
                .build();
    }

    @Transactional
    public void deleteCartItem(String email, List<DeleteRequest> deleteRequests) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        deleteRequests.forEach(deleteRequest -> {
            CartItem cartItem = cartItemRepository.findById(deleteRequest.cartItemId())
                    .orElseThrow(() -> new RuntimeException("해당 장바구니 항목이 존재하지 않습니다."));

            if (!cartItem.getMember().equals(member)) {
                throw new RuntimeException("본인의 장바구니 항목만 삭제할 수 있습니다.");
            }

            cartItemRepository.delete(cartItem);
        });
    }

    @Transactional
    public void updateCartItem(String email, UpdateCartItemRequestDto requestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 사용자가 없습니다."));
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("해당 상품은 주문이 불가능한 상품입니다."));
        CartItem cartItem = cartItemRepository.findById(requestDto.getCartItemId())
                .orElseThrow(() -> new RuntimeException("해당 상품이 장바구니에 없습니다."));

        if (!cartItem.getMember().equals(member)) {
            throw new RuntimeException("본인의 장바구니 항목만 수정할 수 있습니다.");
        }

        if (requestDto.getQuantity() <= 0) {
            throw new RuntimeException("수량은 1개 이상이어야 합니다.");
        }

        Size size = requestDto.getSize();
        if (size.equals(cartItem.getProductItem().getSize())) {
            cartItem.setQuantity(requestDto.getQuantity());
            cartItemRepository.save(cartItem);
        }
        else {
            ProductItem updateItem = productItemRepository.findByProductAndSize(product, requestDto.getSize())
                    .orElse(null);
            CartItem existingItem = cartItemRepository.findByMemberAndProductItem(member, updateItem).orElse(null);
            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + requestDto.getQuantity());
                cartItemRepository.save(existingItem);
                cartItemRepository.delete(cartItem);
                return;
            }

            CartItem newItem = CartItem.builder()
                    .member(member)
                    .productItem(updateItem)
                    .quantity(requestDto.getQuantity())
                    .addedAt(LocalDateTime.now())
                    .build();

            cartItemRepository.save(newItem);
            cartItemRepository.delete(cartItem);
        }

    }
    private Item toItemDto(CartItem cartItem) {
        ProductItem productItem = cartItem.getProductItem();
        Product product = productItem.getProduct();

        List<Size> availableSizes = productItemRepository.findByProduct(product).stream()
                .map(ProductItem::getSize)
                .toList();

        String thumbnailUrl = productImageRepository.findByProductAndIsThumbnailTrue(product)
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return Item.builder()
                .cartItemId(cartItem.getCartItemId())
                .productId(product.getProductId())                      // 상품 ID
                .productItemId(productItem.getProductItemId())
                .productName(product.getProductName())
                .size(productItem.getSize())
                .quantity(cartItem.getQuantity())
                .price(product.getPrice())                              // 원가 기준
                .imageUrl(thumbnailUrl)
                .availableSizes(availableSizes)                         // 가능한 사이즈 리스트
                .build();
    }
}
