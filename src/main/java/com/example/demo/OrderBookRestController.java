package com.example.demo;


import com.example.demo.model.ExecutionStatus;
import com.example.model.LimitOrderDTO;
import com.example.model.OrderStatisticsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
//TODO: error handling
@RestController
public class OrderBookRestController {

    public static final int MAX_TICKER_LENGTH = 5;
    OrderBookService orderBookService;

    public OrderBookRestController(OrderBookService orderBookService){
        this.orderBookService = orderBookService;
    }

    @GetMapping("/hello")
    public String index() {
        return "Kisses from Spring Boot! ;-)";
    }

    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createOrder(@RequestBody LimitOrderDTO createOrder) {
        ExecutionStatus status = orderBookService.processOrder(createOrder);

        return ResponseEntity.ok("Order was successfully processed with status: "+status);

    }

    @PutMapping(path = "/cancel/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancelOrder(@PathVariable("id") Long id) {
        orderBookService.cancelOrder(id);

        return ResponseEntity.ok("Order was successfully added");

    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<LimitOrderDTO> deleteOrder(@PathVariable("id") Long id) {
        Optional<LimitOrderDTO> order = this.orderBookService.deleteOrder(id);
        if (order.isPresent()) {
            return new ResponseEntity<>(order.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<LimitOrderDTO> findOrderById(@PathVariable("id") final Long id) {
        Optional<LimitOrderDTO> order = this.orderBookService.findOrderById(id);
        if (order.isPresent()) {
            return new ResponseEntity<>(order.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(order.get(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<List<LimitOrderDTO>> findAllOrdersForOrderBook(@RequestParam("ticker") final String ticker) {
        if (ticker.length() > MAX_TICKER_LENGTH) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        List<LimitOrderDTO> orders = orderBookService.getOrders(ticker);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<OrderStatisticsDTO> getOrderSummaryForOrderBookByDate(@RequestParam("ticker") final String ticker, @RequestParam("date") final LocalDate date) {
        if (ticker.length() > MAX_TICKER_LENGTH) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        //TODO: handle null return value better!
        OrderStatisticsDTO orderStatisticsDto = orderBookService.getOrderSummaryByDate(ticker, date);
            return new ResponseEntity<>(orderStatisticsDto, HttpStatus.OK);
    }
}


