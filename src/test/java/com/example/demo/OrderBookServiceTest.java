package com.example.demo;

import com.example.model.domain.*;
import com.example.model.db.LimitOrder;
import com.example.model.db.Order;
import com.example.model.db.PriceInformation;
import com.example.model.dto.OrderDTOMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderBookServiceTest {
    OrderBookService orderBookService;
    @MockBean
    OrderRepository orderRepository;

    private static final String TICKER_AAPL = "AAPL";
    private static final String TICKER_SAVE = "SAVE";

    @BeforeEach
    public void init() {
        orderBookService = new OrderBookService(orderRepository);
        orderBookService.registerOrderBook(new OrderBook(TICKER_AAPL));
    }

    //TODO: Test that get quantity returns an aggregated value, for total and each side

    @Test
    public void testFullMatchWithTwoEqualOrders() {
        Order order = createLimitOrder(100.0, 10.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(order, ExecutionStatus.NOT_MATCHED);

        Order order2 = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order2, ExecutionStatus.MATCHED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(100.0).doubleValue());

    }

    private BigDecimal getTotalQuantityForPriceLevel(double price) {
        PriceInformation priceInformation = new PriceInformation();
        priceInformation.setPrice(new BigDecimal(price));
        priceInformation.setCurrency(Currency.SEK);
        return orderBookService.getTotalQuantityForPriceLevel(TICKER_AAPL, priceInformation);
    }

    @Test
    public void testFullMatchWithTwoBuyOrdersEqualToOneSellOrder() {
        Order buyOrder1 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, ExecutionStatus.NOT_MATCHED);

        Order buyOrder2 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, ExecutionStatus.NOT_MATCHED);

        Order sellOrder = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, ExecutionStatus.MATCHED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(100.0).doubleValue());
    }

    @Test
    public void testFullMatchWithSandwich() {
        Order expectedOrder = createLimitOrder(100.0, 5.0, OrderSide.SELL, TICKER_AAPL);

        Order buyOrder1 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, ExecutionStatus.NOT_MATCHED);

        Order sellOrder = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        when(orderRepository.save(eq(expectedOrder))).thenReturn(expectedOrder);
        assertOrderStatus(sellOrder, ExecutionStatus.PARTIALLY_MATCHED);

        Order buyOrder2 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, ExecutionStatus.MATCHED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(100.0).doubleValue());
    }

    @Test
    public void testPartialMatchOnDifferentPriceLevels() {
        Order expectedBuyOrder = createLimitOrder(110.0, 30.0, OrderSide.BUY, TICKER_AAPL);

        Order order1 = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order1, ExecutionStatus.NOT_MATCHED);

        Order order2 = createLimitOrder(110.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order2, ExecutionStatus.NOT_MATCHED);

        Order order3 = createLimitOrder(120.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order3, ExecutionStatus.NOT_MATCHED);

        Order order4 = createLimitOrder(110.0, 50.0, OrderSide.BUY, TICKER_AAPL);
        when(orderRepository.save(eq(expectedBuyOrder))).thenReturn(expectedBuyOrder);
        assertOrderStatus(order4, ExecutionStatus.PARTIALLY_MATCHED);
        assertEquals(new BigDecimal(30.0).doubleValue(), getTotalQuantityForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(10.0).doubleValue(), getTotalQuantityForPriceLevel(120).doubleValue());

    }

    @Test
    public void bestDealAtBuyTest() throws JsonProcessingException {
        Order sellOrder1 = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder1, ExecutionStatus.NOT_MATCHED);

        Order sellOrder2 = createLimitOrder(110.0, 5.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder2, ExecutionStatus.NOT_MATCHED);

        Order sellOrder3 = createLimitOrder(120.0, 15.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder3, ExecutionStatus.NOT_MATCHED);

        Order buyOrder = createLimitOrder(130.0, 14.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder, ExecutionStatus.PARTIALLY_MATCHED);

        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(100).doubleValue());
        assertEquals(new BigDecimal(1).doubleValue(), getTotalQuantityForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(15).doubleValue(), getTotalQuantityForPriceLevel(120).doubleValue());
        //TODO: Remove below
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(buyOrder);
        System.out.println(json);
    }

    @Test
    public void bestDealAtSellTest() {
        Order buyOrder1 = createLimitOrder(100.0, 10.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, ExecutionStatus.NOT_MATCHED);

        Order buyOrder2 = createLimitOrder(110.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, ExecutionStatus.NOT_MATCHED);

        Order buyOrder3 = createLimitOrder(120.0, 15.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder3, ExecutionStatus.NOT_MATCHED);

        Order sellOrder = createLimitOrder(100.0, 16.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, ExecutionStatus.PARTIALLY_MATCHED);

        assertEquals(new BigDecimal(10.0).doubleValue(), getTotalQuantityForPriceLevel(100).doubleValue());
        assertEquals(new BigDecimal(4.0).doubleValue(), getTotalQuantityForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(120).doubleValue());
    }


    @Test
    public void correctStatusTest() {
        Order expectedBuyOrder = createLimitOrder(100.0, 4.0, OrderSide.BUY, TICKER_AAPL);
        Order sellOrder = createLimitOrder(100.0, 4.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, ExecutionStatus.NOT_MATCHED);

        when(orderRepository.save(eq(expectedBuyOrder))).thenReturn(expectedBuyOrder);
        Order buyOrder = createLimitOrder(100.0, 8.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder, ExecutionStatus.PARTIALLY_MATCHED);
    }

    private Order createLimitOrder(Double price, Double qty, OrderSide buyOrSell, String ticker) {

        PriceInformation priceInformation = new PriceInformation(new BigDecimal(price), Currency.SEK);
        return new LimitOrder(priceInformation, new BigDecimal(qty), buyOrSell, ticker, OrderStatus.OPEN);
               /* LimitOrder.builder().priceInformation(priceInformation).
                quantity(new BigDecimal(qty)).side(buyOrSell).ticker(ticker).orderStatus(OrderStatus.OPEN).build();*/
    }

    private void assertOrderStatus(Order order, ExecutionStatus expectedStatus) {
        when(orderRepository.save(eq(order))).thenReturn(order);
        ExecutionStatus statusAfterSecondOrder = orderBookService.processOrder(OrderDTOMapper.toDto(order));
        assertEquals(expectedStatus, statusAfterSecondOrder);
    }

}
