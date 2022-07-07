package com.example.model.domain;

import com.example.model.db.PriceInformation;
import com.example.model.interfaces.IOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class OrderBook {
    private String ticker;

    public OrderBook(String ticker) {
        this.ticker = ticker;
    }

    //Reverse order because highest bid = best bid = is at the beginning of the list.
    //LinkedList was chosen because the orders for each price level are added and consumed according to FIFO/time priority.
    private TreeMap<PriceInformation, LinkedList<IOrder>> buyOrders = new TreeMap<>(new PriceInformationComparator().reversed());
    //Best ask = lowest ask = at the beginning of the list.
    private TreeMap<PriceInformation, LinkedList<IOrder>> sellOrders = new TreeMap<>(new PriceInformationComparator());

    public void addOrder(IOrder order) {
        if (order.getSide().isBuy()) {
            LinkedList<IOrder> buyOrderQueue = buyOrders.get(order.getPriceInformation());
            if (buyOrderQueue == null) {
                buyOrderQueue = new LinkedList<>();
                buyOrders.put(order.getPriceInformation(), buyOrderQueue);
            }
            buyOrderQueue.push(order);
        } else {
            LinkedList<IOrder> sellOrderQueue = sellOrders.get(order.getPriceInformation());
            if (sellOrderQueue == null) {
                sellOrderQueue = new LinkedList<>();
                sellOrders.put(order.getPriceInformation(), sellOrderQueue);
            }
            sellOrderQueue.push(order);
        }
    }

    public Optional<IOrder> cancelOrder(IOrder order) {
        if (order.getSide().isBuy()) {
            return remove(order, buyOrders);
        } else {
            return remove(order, sellOrders);
        }
    }

    public Map<ExecutionAction, List<IOrder>> executeOrder(IOrder order) {
        if (order.getSide().isBuy()) {
            return order.matchAgainstExistingOrders(order, sellOrders);
        } else {
            return order.matchAgainstExistingOrders(order, buyOrders);
        }
    }

    public BigDecimal getTotalQuantityForPriceLevel(PriceInformation price) {
        BigDecimal aggregatedBuyQuantity = getTotalQuantityForPriceLevel(buyOrders, price);
        BigDecimal aggregatedSellQuantity = getTotalQuantityForPriceLevel(sellOrders, price);
        BigDecimal buy = (buyOrders.get(price) != null) ? aggregatedBuyQuantity : BigDecimal.ZERO;
        BigDecimal sell = (sellOrders.get(price) != null) ? aggregatedSellQuantity : BigDecimal.ZERO;

        return buy.add(sell);
    }

    public BigDecimal getQuantityForSideAndPriceLevel(PriceInformation priceInformation, OrderSide side) {
        return side.isBuy() ? getTotalQuantityForPriceLevel(buyOrders, priceInformation) : getTotalQuantityForPriceLevel(sellOrders, priceInformation);
    }

    public boolean isEmpty(OrderSide side) {
        return side.isBuy() ? sellOrders.isEmpty() : buyOrders.isEmpty();
    }

    private Optional<IOrder> remove(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orderTreeMap) {
        IOrder removedOrder = null;
        Iterator<Map.Entry<PriceInformation, LinkedList<IOrder>>> iterator = orderTreeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            LinkedList<IOrder> nextOrderQueue = iterator.next().getValue();
            Iterator<IOrder> queueIterator = nextOrderQueue.iterator();
            while (queueIterator.hasNext()) {
                IOrder next = queueIterator.next();
                if (next.equals(order)) {
                    queueIterator.remove();
                    removedOrder = next;
                    break;
                }
            }
        }
        return Optional.ofNullable(removedOrder);
    }

    private BigDecimal getTotalQuantityForPriceLevel(TreeMap<PriceInformation, LinkedList<IOrder>> orders, PriceInformation priceInformation) {
        LinkedList<IOrder> orderQueue = orders.get(priceInformation);
        if (orderQueue == null || orderQueue.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return orderQueue.stream().map(t -> t.getQuantity()).collect(Collectors.toList()).
                stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    }

}
