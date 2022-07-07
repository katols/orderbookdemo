package com.example.demo;

import com.example.demo.model.ExecutionStatus;
import com.example.model.LimitOrder;
import com.example.model.OrderDTOMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderConsumer {
    public static final int MAX_CAPACITY = 50; //Depends on desired latency

    private static final Logger logger = Logger.getLogger(OrderConsumer.class.getName());
    private List<Long> timeRecordsConsumerSide = new ArrayList<>();
    private List<Long> timeRecordsProducerSide = new ArrayList<>();
    private static int consumedMessages = 0;

    private LinkedBlockingQueue<LimitOrder> orderQueue;
    private OrderBookService orderBookService;
    private AtomicBoolean running = new AtomicBoolean(false);
    private int timeout = 5000; //Default, ms


    public OrderConsumer() {
        orderQueue = new LinkedBlockingQueue(MAX_CAPACITY);
    }

    public void acceptOrder(LimitOrder order) {
        try {
            long acceptTimePre = System.nanoTime();
            this.orderQueue.put(order);
            long acceptTimePost = System.nanoTime();
            timeRecordsProducerSide.add(acceptTimePost - acceptTimePre);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    public void run() {
        while (running.get()) {
            try {
                LimitOrder order = orderQueue.poll(timeout, TimeUnit.MILLISECONDS);
                if (order == null) {
                    break;
                }
                ExecutionStatus status = processOrder(order);

                logger.log(Level.FINE, "Consumed: " + order + " with status: " + status);
                consumedMessages++;
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        logger.log(Level.INFO, "Done consuming. Consumed " + consumedMessages + " messages.");
    }


    public void initialize() {
        this.running.set(true);
    }

    public void shutDown() {
        //calculateAndPrintAverageLatency();
        this.running.set(false);
    }

    private ExecutionStatus processOrder(LimitOrder order) {
        long timestampPre = System.nanoTime();
        ExecutionStatus status = orderBookService.processOrder(OrderDTOMapper.toDto(order));
        long timestampPost = System.nanoTime();
        timeRecordsConsumerSide.add(timestampPost - timestampPre);
        return status;
    }
}

 /*   private void calculateAndPrintAverageLatency() {
        OptionalDouble averageProcessingTime = timeRecordsConsumerSide.stream().mapToLong(Long::longValue).average();
        double averageInMicroSeconds = averageProcessingTime.getAsDouble() / 1000.0;
        logger.log(Level.INFO, "Average processing latency on consumer side is " + averageInMicroSeconds + " microseconds");
        logger.log(Level.INFO, "Maximum processing latency on consumer side:  " + Collections.max(timeRecordsConsumerSide).doubleValue() / 1000.0 + " microseconds");
        logger.log(Level.INFO, "Maximum waiting time of producer: " + Collections.max(timeRecordsProducerSide).doubleValue() / 1000.0 + "microseconds");
    }
}*/
