package com.example.demo;

import com.example.model.Currency;
import com.example.model.LimitOrder;
import com.example.model.OrderSide;
import com.example.model.PriceInformation;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderProducer implements Runnable {

    private static final Logger logger = Logger.getLogger(OrderProducer.class.getName());
    private static int producedMessges = 0;

    private final OrderConsumer orderConsumer;

    public OrderProducer(OrderConsumer orderProcessor) {
        this.orderConsumer = orderProcessor;
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            try {
                produceOrder();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        //logger.log(Level.INFO, "Done producing. Produced " + producedMessges + " messages.");
    }

    public void produceOrder() {
         LimitOrder order = createRandomOrder();
        this.orderConsumer.acceptOrder(order);
        producedMessges++;
      //  logger.log(Level.FINE, "Produced: " + order);
    }

    private LimitOrder createRandomOrder() {
        boolean buy = (Math.random() * 10) >= 5 ? true : false;
        OrderSide side = buy? OrderSide.BUY : OrderSide.SELL;
        return new LimitOrder(new PriceInformation(), new BigDecimal(10.0),side,"tick");
    }

}
