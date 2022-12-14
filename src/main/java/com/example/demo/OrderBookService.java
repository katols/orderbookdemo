package com.example.demo;

import com.example.model.domain.*;
import com.example.model.db.Order;
import com.example.model.db.PriceInformation;
import com.example.model.dto.LimitOrderDTO;
import com.example.model.dto.OrderDTOMapper;
import com.example.model.dto.OrderSummaryDTO;
import com.example.model.interfaces.IOrder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderBookService {
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final OrderRepository orderRepository;

    public OrderBookService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void registerOrderBook(OrderBook orderBook) {
        this.orderBooks.put(orderBook.getTicker(), orderBook);
    }

    public LimitOrderDTO processOrder(LimitOrderDTO createOrder) throws OrderbookException {
        OrderBook orderBook = orderBooks.get(createOrder.getTicker());
        if (orderBook == null) {
            createOrder.setOrderStatus(OrderStatus.OPEN);
            throw new OrderbookException("Orderbook does not exist for this order.");
        }

        if (createOrder.getVolume().signum() == -1) {
            throw new IllegalArgumentException("Order must have a positive volume.");
        }

        Order order = orderRepository.save(OrderDTOMapper.fromDto(createOrder));

        if (orderBook.isEmpty(order.getSide())) {
            orderBook.addOrder(order);
            return OrderDTOMapper.toDto(order);
        }

        Map<ExecutionAction, List<IOrder>> changedOrders = orderBook.executeOrder(order);
        persistChangedOrders(changedOrders, orderBook);
        return OrderDTOMapper.toDto(order);

    }

    public BigDecimal getTotalVolumeForPriceLevel(String ticker, PriceInformation orderValue) {
        return this.orderBooks.get(ticker).getTotalVolumeForPriceLevel(orderValue);
    }

    public List<LimitOrderDTO> getOrders(String ticker) {
        List<Order> searchResult = orderRepository.search(ticker);
        if (searchResult != null && !searchResult.isEmpty()) {
            return orderRepository.search(ticker).stream().map(OrderDTOMapper::toDto).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public LimitOrderDTO cancelOrder(Long id) throws OrderbookException {
        LimitOrderDTO byId;
        Optional<Order> searchResult = orderRepository.findById(id);
        if (searchResult.isPresent()) {
            searchResult.get().setOrderStatus(OrderStatus.CANCELLED);
            orderBooks.get(searchResult.get().getTicker()).cancelOrder(searchResult.get());
            orderRepository.save(searchResult.get());
            byId = OrderDTOMapper.toDto(searchResult.get());
        } else {
            throw new OrderbookException("Order to be deleted does not exist.");
        }
        return byId;
    }

    public Optional<LimitOrderDTO> findOrderById(Long id) {
        Optional<LimitOrderDTO> byId = Optional.empty();
        Optional<Order> searchResult = orderRepository.findById(id);
        if (searchResult.isPresent()) {
            byId = Optional.ofNullable(OrderDTOMapper.toDto(searchResult.get()));
        }
        return byId;
    }

    public OrderSummaryDTO getOrderSummaryByDate(String ticker, LocalDate date, OrderSide side) {

        List<Order> matchingOrders;
        OrderSummaryDTO statistics = null;
        List<Order> searchResult = orderRepository.search(ticker);
        if (searchResult != null && !searchResult.isEmpty()) {
            matchingOrders = searchResult.stream().
                    filter(t -> dateEquals(t.getCreationTime(), date)).
                    filter(t -> t.getSide().equals(side)).collect(Collectors.toList());
            if (!matchingOrders.isEmpty()) {
                statistics = calculateOrderStatistics(matchingOrders);
            }
        }
        return statistics;
    }

    private void persistChangedOrders(Map<ExecutionAction, List<IOrder>> changedOrders, OrderBook orderBook) {
        changedOrders.forEach((key, value) -> value.forEach(order -> {
                    Order newOrder = orderRepository.save((Order) order);
                    if (key == ExecutionAction.ADD || key == ExecutionAction.PARTIAL_ADD) {
                        orderBook.addOrder(newOrder);
                    }
                }
        ));
    }

    //Calculate min, max, average and qty of orders for that side for a given ticker and date
    private OrderSummaryDTO calculateOrderStatistics(List<Order> matchingOrders) {
        BigDecimal minPrice = matchingOrders.stream().map(t -> t.getPriceInformation().getPrice()).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = matchingOrders.stream().map(t -> t.getPriceInformation().getPrice()).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        List<BigDecimal> weightedQuantities = matchingOrders.stream().map(Order::getVolume).collect(Collectors.toList());
        List<BigDecimal> weightedPrices = matchingOrders.stream().map(t -> t.getPriceInformation().getPrice().multiply(t.getVolume())).collect(Collectors.toList());
        BigDecimal priceSum = weightedPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal qtySum = weightedQuantities.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePrice = priceSum.divide(qtySum, RoundingMode.CEILING);
        int noOfOrders = matchingOrders.size();


        return new OrderSummaryDTO(minPrice, maxPrice, averagePrice, noOfOrders);
    }

    private boolean dateEquals(Timestamp t, LocalDate date) {
        return t.toLocalDateTime().toLocalDate().equals(date);
    }
}
