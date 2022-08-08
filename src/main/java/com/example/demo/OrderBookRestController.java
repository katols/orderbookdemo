package com.example.demo;


import com.example.model.domain.OrderSide;
import com.example.model.dto.LimitOrderDTO;
import com.example.model.dto.OrderSummaryDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderBookRestController {

    public static final int MAX_TICKER_LENGTH = 5;
    OrderBookService orderBookService;

    public OrderBookRestController(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LimitOrderDTO> createOrder(@RequestBody LimitOrderDTO createOrder) {
        LimitOrderDTO order;
        try {
            order = orderBookService.processOrder(createOrder);
        } catch (OrderbookException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException f) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, f.getMessage(), f);
        }
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PutMapping(path = "/cancel/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LimitOrderDTO> cancelOrder(@PathVariable("id") Long id) {
        LimitOrderDTO order;
        try {
            order = orderBookService.cancelOrder(id);
        } catch (OrderbookException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LimitOrderDTO> findOrderById(@PathVariable("id") final Long id) {
        Optional<LimitOrderDTO> order = this.orderBookService.findOrderById(id);
        if (order.isPresent()) {
            return new ResponseEntity<>(order.get(), HttpStatus.OK);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order does not exist");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<LimitOrderDTO>> findAllOrdersForOrderBook(@RequestParam("ticker") final String ticker) {
        if (ticker.length() > MAX_TICKER_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticker length is maximum" + MAX_TICKER_LENGTH + "characters.");
        }
        List<LimitOrderDTO> orders = orderBookService.getOrders(ticker);
        if (orders.isEmpty()) {
            return new ResponseEntity<>(orders, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/ordersummary")
    public ResponseEntity<OrderSummaryDTO> getOrderSummaryForOrderBookByDate(@RequestParam("ticker") final String ticker,
                                                                             @RequestParam("date") final LocalDate date,
                                                                             @RequestParam("side") final OrderSide side) {
        if (ticker.length() > MAX_TICKER_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticker length is maximum" + MAX_TICKER_LENGTH + "characters.");
        }
        OrderSummaryDTO orderStatisticsDto = orderBookService.getOrderSummaryByDate(ticker, date, side);
        if (orderStatisticsDto == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(orderStatisticsDto, HttpStatus.OK);
    }
}


