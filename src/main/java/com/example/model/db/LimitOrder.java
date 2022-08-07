package com.example.model.db;

import com.example.model.domain.ExecutionAction;
import com.example.model.domain.OrderSide;
import com.example.model.interfaces.IOrder;
import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.util.*;

@Data
@Entity
@DiscriminatorValue("limit")
public class LimitOrder extends Order {

    public LimitOrder(PriceInformation priceInformation, BigDecimal volume, OrderSide orderSide, String ticker) {
        super(priceInformation, volume, orderSide, ticker);
    }

    public LimitOrder() {

    }

    @Override
    public Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orders) {
        {
            Map<ExecutionAction, List<IOrder>> changedOrders = initializeChangedOrders();
            BigDecimal qtyAddedOrRemoved = order.getVolume();
            Iterator<Map.Entry<PriceInformation, LinkedList<IOrder>>> iterator = orders.entrySet().iterator();
            while (iterator.hasNext() && qtyAddedOrRemoved.signum() == 1) {
                Map.Entry<PriceInformation, LinkedList<IOrder>> nextEntry = iterator.next();
                if (isMatch(order, nextEntry.getKey())) {
                    LinkedList<IOrder> nextOrderQueue = nextEntry.getValue();
                    Iterator<IOrder> queueIterator = nextOrderQueue.iterator();
                    while (queueIterator.hasNext() && qtyAddedOrRemoved.signum() == 1) {
                        IOrder next = queueIterator.next();
                        BigDecimal remaining = next.getVolume().subtract(qtyAddedOrRemoved);
                        if (remaining.signum() <= 0) {
                            closeOrder(queueIterator, next);
                            qtyAddedOrRemoved = remaining.abs();
                            updateChangedOrders(changedOrders, ExecutionAction.CLOSE, next); //Tell the repository that order was updated to closed
                            if (remaining.signum() == 0) {
                                updateChangedOrders(changedOrders, ExecutionAction.CLOSE, order); //Tell the repository that order was updated to closed
                            }
                        } else {
                            next.updateVolume(remaining); //Om remaining >0 och side = other side = lägg ej till en ny order utan reducera den gamla. Inkommande order för liten.
                            updateChangedOrders(changedOrders, ExecutionAction.UPDATE, next); //Tell the repository that order was updated to new qty
                            updateChangedOrders(changedOrders, ExecutionAction.CLOSE, order);
                            return changedOrders;
                        }
                    }
                }
            }
            if (qtyAddedOrRemoved.signum() == 1) {
                if (qtyAddedOrRemoved.compareTo(order.getVolume()) == 0) {
                    order.updateVolume(qtyAddedOrRemoved.abs());
                    updateChangedOrders(changedOrders, ExecutionAction.ADD, order);
                } else {
                    order.updateVolume(qtyAddedOrRemoved.abs());
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
        return "Price: " + this.getPriceInformation() + " Volume: " + this.getVolume() + " Side: " + this.getSide();
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + getPriceInformation().getPrice().intValue();
        result = prime * result + getVolume().intValue();
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
                order.getVolume().compareTo(this.getVolume()) == 0
                && order.getTicker().equals(this.getTicker())
                && order.getSide() == this.getSide()
                && order.getOrderStatus() == this.getOrderStatus();
    }

}
