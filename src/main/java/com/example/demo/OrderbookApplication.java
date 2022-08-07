package com.example.demo;

import com.example.model.domain.OrderBook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

//TODO: Cleanup. 1: Address all TODOs,
// 2: Remove all fix formatting, fix naming, remove unused imports, remove commented lines, remove unused Classes
// 3: Place all methods in proper accessor order (public/private)
// 4: Swagger
// 5: Rest API tests

@SpringBootApplication
@EnableJpaRepositories("com.example.*")
@EntityScan("com.example.model")
public class OrderbookApplication {
    private OrderBookService orderBookService;

    public static void main(String[] args) {
        SpringApplication.run(OrderbookApplication.class, args);

    }

    public OrderbookApplication(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
        //TODO: Maybe Fix this registration. Maybe add an xml?
        orderBookService.registerOrderBook(new OrderBook("AAPL"));
        orderBookService.registerOrderBook(new OrderBook("SAVE"));
        orderBookService.registerOrderBook(new OrderBook("TSLA"));
    }

}
