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
            LinkedList<IOrder> buyOrderQueue = buyOrders.computeIfAbsent(order.getPriceInformation(), k -> new LinkedList<>());
            buyOrderQueue.push(order);
        } else {
            LinkedList<IOrder> sellOrderQueue = sellOrders.computeIfAbsent(order.getPriceInformation(), k -> new LinkedList<>());
            sellOrderQueue.push(order);
        }
    }

    public void cancelOrder(IOrder order) {
        if (order.getSide().isBuy()) {
            remove(order, buyOrders);
        } else {
            remove(order, sellOrders);
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

    public boolean isEmpty(OrderSide side) {
        return side.isBuy() ? sellOrders.isEmpty() : buyOrders.isEmpty();
    }

    private void remove(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orderTreeMap) {
        for (Map.Entry<PriceInformation, LinkedList<IOrder>> priceInformationLinkedListEntry : orderTreeMap.entrySet()) {
            LinkedList<IOrder> nextOrderQueue = priceInformationLinkedListEntry.getValue();
            Iterator<IOrder> queueIterator = nextOrderQueue.iterator();
            while (queueIterator.hasNext()) {
                IOrder next = queueIterator.next();
                if (next.equals(order)) {
                    queueIterator.remove();
                    break;
                }
            }
        }
    }

    private BigDecimal getTotalQuantityForPriceLevel(TreeMap<PriceInformation, LinkedList<IOrder>> orders, PriceInformation priceInformation) {
        LinkedList<IOrder> orderQueue = orders.get(priceInformation);
        if (orderQueue == null || orderQueue.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return orderQueue.stream().map(IOrder::getQuantity).collect(Collectors.toList()).
                stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    }

}
