package com.example.demo;

import com.example.model.domain.*;
import com.example.model.db.Order;
import com.example.model.db.PriceInformation;
import com.example.model.dto.LimitOrderDTO;
import com.example.model.dto.OrderDTOMapper;
import com.example.model.dto.OrderStatisticsDTO;
import com.example.model.interfaces.IOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderBookService {
    private Map<String, OrderBook> orderBooks = new HashMap<>();
    private OrderRepository orderRepository;

    public OrderBookService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void registerOrderBook(OrderBook orderBook) {
        this.orderBooks.put(orderBook.getTicker(), orderBook);
    }

    public ExecutionStatus processOrder(LimitOrderDTO createOrder) {
        OrderBook orderBook = orderBooks.get(createOrder.getTicker());
        if (orderBook == null) {
            return ExecutionStatus.NOT_MATCHED;
        }
        Order order = orderRepository.save(OrderDTOMapper.fromDto(createOrder));

        if (order.getQuantity().signum() == -1) {
            throw new IllegalArgumentException("Order must have a positive quantity.");
        }

        if (orderBook.isEmpty(order.getSide())) {
            orderBook.addOrder(order);
            return ExecutionStatus.NOT_MATCHED;
        }

        Map<ExecutionAction, List<IOrder>> changedOrders = orderBook.executeOrder(order);

        persistChangedOrders(changedOrders, orderBook); //
        ExecutionStatus executionStatus = determineStatus(changedOrders);

        return executionStatus;

    }

    public BigDecimal getTotalQuantityForPriceLevel(String ticker, PriceInformation orderValue) {
        return this.orderBooks.get(ticker).getTotalQuantityForPriceLevel(orderValue);
    }

    public BigDecimal getQuantityForSideAndPriceLevel(String ticker, PriceInformation price, OrderSide side){
        return this.orderBooks.get(ticker).getQuantityForSideAndPriceLevel(price, side);
    }

    public List<LimitOrderDTO> getOrders(String ticker) {
        List<Order> searchResult = orderRepository.search(ticker);
        if (searchResult != null && !searchResult.isEmpty()) { //TODO: can we use Optional here instead?
            return orderRepository.search(ticker).stream().map(t -> OrderDTOMapper.toDto(t)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
//TODO: Fugly casting?
    public Optional<LimitOrderDTO> deleteOrder(Long id) {
        Optional<LimitOrderDTO> searchResult = null;
        Optional<Order> existingOrder = orderRepository.findById(id);
        if(existingOrder.isPresent() && existingOrder.get() instanceof Order){
            orderBooks.get(existingOrder.get().getTicker()).cancelOrder(existingOrder.get());
            orderRepository.delete(existingOrder.get());
            searchResult = Optional.ofNullable(OrderDTOMapper.toDto((Order) existingOrder.get()));
        }
        return searchResult;
    }

    public Optional<LimitOrderDTO> cancelOrder(Long id) {
        Optional<LimitOrderDTO> byId = null;
        Optional<Order> searchResult = orderRepository.findById(id);
        if (searchResult.isPresent() && searchResult.get() instanceof Order) {
            searchResult.get().setOrderStatus(OrderStatus.CANCELLED);
            orderBooks.get(searchResult.get().getTicker()).cancelOrder(searchResult.get());
            orderRepository.save(searchResult.get());
            byId = Optional.ofNullable(OrderDTOMapper.toDto((Order) searchResult.get()));
        }
        return byId;
    }

    public Optional<LimitOrderDTO> findOrderById(Long id) {

        Optional<LimitOrderDTO> byId = null;
        Optional<Order> searchResult = orderRepository.findById(id);
        if(searchResult.isPresent() && searchResult.get() instanceof Order){
            byId = Optional.ofNullable(OrderDTOMapper.toDto((Order) searchResult.get()));
        }
        return byId;
    }

    public OrderStatisticsDTO getOrderSummaryByDate(String ticker, LocalDate date) {

        List<Order> matchingOrders = null;
        List<Order> searchResult = orderRepository.search(ticker);
        if (searchResult != null && !searchResult.isEmpty()) {
            matchingOrders = searchResult.stream().filter(t -> dateEquals(t.getCreationTime(), date)).collect(Collectors.toList());
        }
        OrderStatisticsDTO statistics = calculateOrderStatistics(matchingOrders);


        return statistics;
    }

    private void persistChangedOrders(Map<ExecutionAction, List<IOrder>> changedOrders, OrderBook orderBook) {
        changedOrders.entrySet().stream().forEach(orderList ->
                orderList.getValue().stream().forEach(order -> {
                            Optional<Order> byId = orderRepository.findById(order.getId());
                            if (byId.isPresent()) {
                                //TODO: log this
                                System.out.println("updating order with id " + order);
                            }
                            Order newOrder = orderRepository.save((Order) order);
                            if (orderList.getKey() == ExecutionAction.ADD) {
                                orderBook.addOrder(newOrder);
                            }
                        }
                ));
    }

    //If an entry with closed status exists, a partial or full match took place.
    //If above is true and add exists, or if other update exists, then a partial match took place. if full match took place, only closed status order(s) exist.
    //changedorders should never be empty.
    private ExecutionStatus determineStatus(Map<ExecutionAction, List<IOrder>> changedOrders) {
        List<IOrder> closedOrders = changedOrders.get(ExecutionAction.CLOSE);
        if (!closedOrders.isEmpty()) { //partial or full match
            if (changedOrders.get(ExecutionAction.UPDATE).isEmpty() && changedOrders.get(ExecutionAction.ADD).isEmpty()) {
                return ExecutionStatus.MATCHED;
            } else {
                return ExecutionStatus.PARTIALLY_MATCHED;
            }
        } else {
            return ExecutionStatus.NOT_MATCHED;
        }
    }

    private OrderStatisticsDTO calculateOrderStatistics(List<Order> matchingOrders) {
        return new OrderStatisticsDTO();
    }

    private boolean dateEquals(Timestamp t, LocalDate date) {
        //t.toInstant().atZone(ZoneId.of("UTC")).toLocalDate().equals(date);
        return t.toLocalDateTime().toLocalDate().equals(date);
    }
}
