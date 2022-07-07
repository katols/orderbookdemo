package com.example.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@Entity
//TODO: Investigate builder
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LimitOrder implements IOrder {
    //TODO: fix id sequence
    //TODO: Replace priceLimit with PriceInformation, create associated comparator
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

    public LimitOrder(PriceInformation priceInformation, BigDecimal quantity, OrderSide orderSide, String ticker){
      this.priceInformation = priceInformation;
      this.quantity = quantity;
      this.side = orderSide;
      this.ticker = ticker;
    }

    @Override
    public LimitOrder create(PriceInformation priceInformation, BigDecimal qtyAddedOrRemoved, OrderSide side, String ticker) {
        return new LimitOrder(priceInformation, qtyAddedOrRemoved, side, ticker);
    }
    public PriceInformation getPriceInformation() {
        return this.priceInformation;
    }

    //TODO: Should the order be immutable or not?
    @Override
    public void updateQuantity(BigDecimal remaining) {
        this.setQuantity(remaining);
    }

    public void setPriceInformation(PriceInformation priceInformation) {
        this.priceInformation = priceInformation;
    }

    @PrePersist
    private void createTimeStamp(){
        this.creationTime = Timestamp.from(ZonedDateTime.from(ZonedDateTime.now(ZoneId.of("UTC"))).toInstant());
    }

    @Override
    public String toString() {
        return "Price: " + this.priceInformation + " Quantity: " + this.quantity + " Side: " + this.side;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (int) priceInformation.getPrice().intValue();
        result = prime * result + quantity.intValue();
        return result;
    }
//TODO: finish this equals method
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LimitOrder)) {
            return false;
        }

        LimitOrder order = (LimitOrder) o;

        return order.priceInformation.equals(this.getPriceInformation()) &&
                order.quantity.compareTo(this.quantity) == 0
                && order.ticker.equals(this.ticker)
                && order.side == this.side
                && order.orderStatus == this.orderStatus;
    }

}
