package com.example.model.db;

import com.example.model.domain.ExecutionAction;
import com.example.model.interfaces.IOrder;
import com.example.model.domain.OrderSide;
import com.example.model.domain.OrderStatus;
import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.util.*;

@Data
@Entity
@DiscriminatorValue("limit")
public class LimitOrder extends Order {

    public LimitOrder(PriceInformation priceInformation, BigDecimal quantity, OrderSide orderSide, String ticker, OrderStatus orderStatus) {
        super(priceInformation, quantity, orderSide, ticker, orderStatus);
    }

    public LimitOrder() {

    }

    @Override
    public Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orders) {
        {
            Map<ExecutionAction, List<IOrder>> changedOrders = initializeChangedOrders();
            BigDecimal qtyAddedOrRemoved = order.getQuantity();
            Iterator<Map.Entry<PriceInformation, LinkedList<IOrder>>> iterator = orders.entrySet().iterator();
            while (iterator.hasNext() && qtyAddedOrRemoved.signum() == 1) {
                Map.Entry<PriceInformation, LinkedList<IOrder>> nextEntry = iterator.next();
                if (isMatch(order, nextEntry.getKey())) {
                    LinkedList<IOrder> nextOrderQueue = nextEntry.getValue();
                    Iterator<IOrder> queueIterator = nextOrderQueue.iterator();
                    while (queueIterator.hasNext() && qtyAddedOrRemoved.signum() == 1) {
                        IOrder next = queueIterator.next();
                        BigDecimal remaining = next.getQuantity().subtract(qtyAddedOrRemoved);
                        if (remaining.signum() <= 0) {
                            closeOrder(queueIterator, next);
                            qtyAddedOrRemoved = remaining.abs();
                            updateChangedOrders(changedOrders, ExecutionAction.CLOSE, next); //Tell the repository that order was updated to closed
                            if (remaining.signum() == 0) {
                                updateChangedOrders(changedOrders, ExecutionAction.CLOSE, order); //Tell the repository that order was updated to closed
                            }
                        } else {
                            next.updateQuantity(remaining); //Om remaining >0 och side = other side = lägg ej till en ny order utan reducera den gamla. Inkommande order för liten.
                            updateChangedOrders(changedOrders, ExecutionAction.UPDATE, next); //Tell the repository that order was updated to new qty
                            updateChangedOrders(changedOrders, ExecutionAction.CLOSE, order);
                            return changedOrders;
                        }
                    }
                }
            }
            if (qtyAddedOrRemoved.signum() == 1) {
                if (qtyAddedOrRemoved.compareTo(order.getQuantity()) == 0) {
                    order.updateQuantity(qtyAddedOrRemoved.abs());
                    updateChangedOrders(changedOrders, ExecutionAction.ADD, order);
                } else {
                    order.updateQuantity(qtyAddedOrRemoved.abs());
                    updateChangedOrders(changedOrders, ExecutionAction.PARTIAL_ADD, order);

                }
            }
            return changedOrders;
        }


    }

    private boolean isMatch(IOrder order, PriceInformation next) {
        return order.getSide().isBuy() ? (order.getPriceInformation().getPrice().compareTo(next.getPrice()) >= 0) : (order.getPriceInformation().getPrice().compareTo(next.getPrice()) <= 0);
    }

    @Override
    public String toString() {
        return "Price: " + this.getPriceInformation() + " Quantity: " + this.getQuantity() + " Side: " + this.getSide();
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + getPriceInformation().getPrice().intValue();
        result = prime * result + getQuantity().intValue();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Order)) {
            return false;
        }

        Order order = (Order) o;

        return order.getPriceInformation().equals(this.getPriceInformation()) &&
                order.getQuantity().compareTo(this.getQuantity()) == 0
                && order.getTicker().equals(this.getTicker())
                && order.getSide() == this.getSide()
                && order.getOrderStatus() == this.getOrderStatus();
    }

}
