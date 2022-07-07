package com.example.model.interfaces;

import com.example.model.domain.ExecutionAction;
import com.example.model.domain.OrderSide;
import com.example.model.domain.OrderStatus;
import com.example.model.db.PriceInformation;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface IOrder {

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
