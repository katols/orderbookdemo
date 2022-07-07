package com.example.model;

import com.example.demo.ExecutionAction;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface IOrder {
    IOrder create(PriceInformation priceInformation,
                  BigDecimal qtyAddedOrRemoved,
                  OrderSide side,
                  String ticker);

    PriceInformation getPriceInformation();

    String getTicker();

    OrderSide getSide();

    BigDecimal getQuantity();

    void updateQuantity(BigDecimal remaining);

    void setOrderStatus(OrderStatus closed);

    Long getId();

    Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order,
                                                                  TreeMap<PriceInformation,
                                                                          LinkedList<IOrder>> orders);

    OrderStatus getOrderStatus();
}
