package com.example.model;

import com.example.demo.ExecutionAction;
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
    //TODO: fix id sequence
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
    private String ticker; //instrument name

    private BigDecimal volume;
    @Column
    private Timestamp creationTime;
    private OrderStatus orderStatus = OrderStatus.OPEN;

    public Order(PriceInformation priceInformation, BigDecimal quantity, OrderSide orderSide, String ticker, OrderStatus orderStatus) {
        this.priceInformation = priceInformation;
        this.quantity = quantity;
        this.side = orderSide;
        this.ticker = ticker;
        this.orderStatus = OrderStatus.OPEN;
    }

    public PriceInformation getPriceInformation() {
        return this.priceInformation;
    }

    //TODO: Should the order be immutable or not?
    @Override
    public void updateQuantity(BigDecimal remaining) {
        this.setQuantity(remaining);
    }
    @Override
    public abstract Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orders);

    void updateChangedOrders(Map<ExecutionAction, List<IOrder>> changedOrders, ExecutionAction action, IOrder next) {
        List<IOrder> orders = changedOrders.get(action);
        if (orders == null) {
            orders = new ArrayList<>();
            changedOrders.put(action, orders);
        }
        orders.add(next);
    }
    Map<ExecutionAction, List<IOrder>> initializeChangedOrders() {
        Map<ExecutionAction, List<IOrder>> changedOrders = new HashMap<>();
        changedOrders.put(ExecutionAction.ADD, new ArrayList<>());
        changedOrders.put(ExecutionAction.CLOSE, new ArrayList<>());
        changedOrders.put(ExecutionAction.UPDATE, new ArrayList<>());
        return changedOrders;
    }

    public void setPriceInformation(PriceInformation priceInformation) {
        this.priceInformation = priceInformation;
    }
    @Override
    public OrderStatus getOrderStatus(){
        return this.orderStatus;
    };

    @PrePersist
    private void createTimeStamp() {
        this.creationTime = Timestamp.from(ZonedDateTime.from(ZonedDateTime.now(ZoneId.of("UTC"))).toInstant());
    }

    void closeOrder(Iterator<IOrder> iterator, IOrder nextOrder) {
        iterator.remove();
        nextOrder.setOrderStatus(OrderStatus.CLOSED);
    }



}
