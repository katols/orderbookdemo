package com.example.model;

import com.example.demo.ExecutionAction;
import lombok.Data;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

//TODO: Double or BigDecimal?
@Data
public class OrderBook {

    private long id;
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
            return matchAgainstExistingOrders(order, sellOrders);
        } else {
            return matchAgainstExistingOrders(order, buyOrders);
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

    private boolean isMatch(IOrder order, PriceInformation next) {
        return order.getSide().isBuy() ? (order.getPriceInformation().getPrice().compareTo(next.getPrice()) >= 0) : (order.getPriceInformation().getPrice().compareTo(next.getPrice()) <= 0);
    }


    private Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orders) {
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
                    if (remaining.signum() == 0 || remaining.signum() == -1) {
                        closeOrder(queueIterator, next);
                        qtyAddedOrRemoved = remaining.abs();
                        updateChangedOrders(changedOrders, ExecutionAction.CLOSE, next); //Tell the repository that order was updated to closed
                    } else {
                        next.updateQuantity(remaining); //Om remaining >0 och side = other side = lägg ej till en ny order utan reducera den gamla. Inkommande order för liten.
                        updateChangedOrders(changedOrders, ExecutionAction.UPDATE, next); //Tell the repository that order was updated to new qty
                        return changedOrders;
                    }
                }
            }
        }
        if (qtyAddedOrRemoved.signum() == 1) {
            updateChangedOrders(changedOrders, ExecutionAction.ADD,
                  order.create(order.getPriceInformation(), qtyAddedOrRemoved, order.getSide(), order.getTicker()));
        }
        return changedOrders;
    }

    private Map<ExecutionAction, List<IOrder>> initializeChangedOrders() {
        Map<ExecutionAction, List<IOrder>> changedOrders = new HashMap<>();
        changedOrders.put(ExecutionAction.ADD, new ArrayList<>());
        changedOrders.put(ExecutionAction.CLOSE, new ArrayList<>());
        changedOrders.put(ExecutionAction.UPDATE, new ArrayList<>());
        return changedOrders;
    }

    private void updateChangedOrders(Map<ExecutionAction, List<IOrder>> changedOrders, ExecutionAction action, IOrder next) {
        List<IOrder> orders = changedOrders.get(action);
        if (orders == null) {
            orders = new ArrayList<>();
            changedOrders.put(action, orders);
        }
        orders.add(next);
    }

    private void closeOrder(Iterator<IOrder> iterator, IOrder nextOrder) {
        iterator.remove();
        nextOrder.setOrderStatus(OrderStatus.CLOSED);
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
