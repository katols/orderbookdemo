package com.example.model.db;

import com.example.model.domain.ExecutionAction;
import com.example.model.interfaces.IOrder;
import com.example.model.domain.OrderSide;
import com.example.model.domain.OrderStatus;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Data
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "orders")
@DiscriminatorColumn(name = "type")
@AllArgsConstructor
@NoArgsConstructor
public abstract class Order implements IOrder {
    @Id
    @GeneratedValue
    private Long id;
    @NonNull
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "price_information_id")
    private PriceInformation priceInformation;
    private BigDecimal quantity;
    @NonNull
    private OrderSide side;
    @NonNull
    private String ticker;

    private BigDecimal volume;
    @Column
    private Timestamp creationTime;
    private OrderStatus orderStatus = OrderStatus.OPEN;

    public Order(PriceInformation priceInformation, BigDecimal quantity, OrderSide orderSide, String ticker) {
        this.priceInformation = priceInformation;
        this.quantity = quantity;
        this.side = orderSide;
        this.ticker = ticker;
    }

    @Override
    public void updateQuantity(BigDecimal remaining) {
        this.setQuantity(remaining);
    }

    @Override
    public abstract Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orders);

    @Override
    public OrderStatus getOrderStatus() {
        return this.orderStatus;
    }

    @Override
    public PriceInformation getPriceInformation() {
        return this.priceInformation;
    }

    void updateChangedOrders(Map<ExecutionAction, List<IOrder>> changedOrders, ExecutionAction action, IOrder order) {
        List<IOrder> orders = changedOrders.computeIfAbsent(action, k -> new ArrayList<>());
        switch (action) {
            case CLOSE:
                order.setOrderStatus(OrderStatus.CLOSED);
                break;
            case UPDATE:
            case PARTIAL_ADD:
                order.setOrderStatus(OrderStatus.PARTIALLY_MATCHED);
                break;
            case ADD:
                order.setOrderStatus(OrderStatus.OPEN);
        }

        orders.add(order);
    }

    Map<ExecutionAction, List<IOrder>> initializeChangedOrders() {
        Map<ExecutionAction, List<IOrder>> changedOrders = new HashMap<>();
        changedOrders.put(ExecutionAction.ADD, new ArrayList<>());
        changedOrders.put(ExecutionAction.CLOSE, new ArrayList<>());
        changedOrders.put(ExecutionAction.UPDATE, new ArrayList<>());
        return changedOrders;
    }

    void closeOrder(Iterator<IOrder> iterator, IOrder nextOrder) {
        iterator.remove();
        nextOrder.setOrderStatus(OrderStatus.CLOSED);
    }

    @PrePersist
    private void createTimeStamp() {
        this.creationTime = Timestamp.from(ZonedDateTime.from(ZonedDateTime.now(ZoneId.of("UTC"))).toInstant());
    }


}
