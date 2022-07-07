package com.example.model;

import java.math.BigDecimal;

public interface IOrder {
    IOrder create(PriceInformation priceInformation,
                  BigDecimal qtyAddedOrRemoved, OrderSide side, String ticker);

    PriceInformation getPriceInformation();

    String getTicker();

    OrderSide getSide();

    BigDecimal getQuantity();

    void updateQuantity(BigDecimal remaining);

    void setOrderStatus(OrderStatus closed);

    Long getId();
}
