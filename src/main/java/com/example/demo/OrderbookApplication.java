package com.example.demo;

import com.example.model.domain.OrderBook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.example.*")
@EntityScan("com.example.model")
public class OrderbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderbookApplication.class, args);

    }

    public OrderbookApplication(OrderBookService orderBookService) {
        orderBookService.registerOrderBook(new OrderBook("AAPL"));
        orderBookService.registerOrderBook(new OrderBook("SAVE"));
        orderBookService.registerOrderBook(new OrderBook("TSLA"));
    }

}
