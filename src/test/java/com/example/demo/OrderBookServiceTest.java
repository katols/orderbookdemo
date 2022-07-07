package com.example.demo;

import com.example.demo.model.ExecutionStatus;
import com.example.model.*;
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
    LimitOrderRepository limitOrderRepository;

    private static final String TICKER_AAPL = "AAPL";
    private static final String TICKER_SAVE = "SAVE";

    @BeforeEach
    public void init() {
        orderBookService = new OrderBookService(limitOrderRepository);
        orderBookService.registerOrderBook(new OrderBook(TICKER_AAPL));
    }

    //TODO: Test that get quantity returns an aggregated value, for total and each side

    @Test
    public void testFullMatchWithTwoEqualOrders() {
        LimitOrder limitOrder = createLimitOrder(100.0, 10.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(limitOrder, ExecutionStatus.NOT_MATCHED);

        LimitOrder order2 = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
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
        LimitOrder buyOrder1 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, ExecutionStatus.NOT_MATCHED);

        LimitOrder buyOrder2 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, ExecutionStatus.NOT_MATCHED);

        LimitOrder sellOrder = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, ExecutionStatus.MATCHED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(100.0).doubleValue());
    }

    @Test
    public void testFullMatchWithSandwich() {
        LimitOrder expectedOrder = createLimitOrder(100.0, 5.0, OrderSide.SELL, TICKER_AAPL);

        LimitOrder buyOrder1 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, ExecutionStatus.NOT_MATCHED);

        LimitOrder sellOrder = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        when(limitOrderRepository.save(eq(expectedOrder))).thenReturn(expectedOrder);
        assertOrderStatus(sellOrder, ExecutionStatus.PARTIALLY_MATCHED);

        LimitOrder buyOrder2 = createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, ExecutionStatus.MATCHED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(100.0).doubleValue());
    }

    @Test
    public void testPartialMatchOnDifferentPriceLevels() {
        LimitOrder expectedBuyOrder = createLimitOrder(110.0, 30.0, OrderSide.BUY, TICKER_AAPL);

        LimitOrder order1 = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order1, ExecutionStatus.NOT_MATCHED);

        LimitOrder order2 = createLimitOrder(110.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order2, ExecutionStatus.NOT_MATCHED);

        LimitOrder order3 = createLimitOrder(120.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order3, ExecutionStatus.NOT_MATCHED);

        LimitOrder order4 = createLimitOrder(110.0, 50.0, OrderSide.BUY, TICKER_AAPL);
        when(limitOrderRepository.save(eq(expectedBuyOrder))).thenReturn(expectedBuyOrder);
        assertOrderStatus(order4, ExecutionStatus.PARTIALLY_MATCHED);
        assertEquals(new BigDecimal(30.0).doubleValue(), getTotalQuantityForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(10.0).doubleValue(), getTotalQuantityForPriceLevel(120).doubleValue());

    }

    @Test
    public void bestDealAtBuyTest() throws JsonProcessingException {
        LimitOrder sellOrder1 = createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder1, ExecutionStatus.NOT_MATCHED);

        LimitOrder sellOrder2 = createLimitOrder(110.0, 5.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder2, ExecutionStatus.NOT_MATCHED);

        LimitOrder sellOrder3 = createLimitOrder(120.0, 15.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder3, ExecutionStatus.NOT_MATCHED);

        LimitOrder buyOrder = createLimitOrder(130.0, 14.0, OrderSide.BUY, TICKER_AAPL);
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
        LimitOrder buyOrder1 = createLimitOrder(100.0, 10.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, ExecutionStatus.NOT_MATCHED);

        LimitOrder buyOrder2 = createLimitOrder(110.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, ExecutionStatus.NOT_MATCHED);

        LimitOrder buyOrder3 = createLimitOrder(120.0, 15.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder3, ExecutionStatus.NOT_MATCHED);

        LimitOrder sellOrder = createLimitOrder(100.0, 16.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, ExecutionStatus.PARTIALLY_MATCHED);

        assertEquals(new BigDecimal(10.0).doubleValue(), getTotalQuantityForPriceLevel(100).doubleValue());
        assertEquals(new BigDecimal(4.0).doubleValue(), getTotalQuantityForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(0).doubleValue(), getTotalQuantityForPriceLevel(120).doubleValue());
    }


    @Test
    public void correctStatusTest() {
        LimitOrder expectedBuyOrder = createLimitOrder(100.0, 4.0, OrderSide.BUY, TICKER_AAPL);
        LimitOrder sellOrder = createLimitOrder(100.0, 4.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, ExecutionStatus.NOT_MATCHED);

        when(limitOrderRepository.save(eq(expectedBuyOrder))).thenReturn(expectedBuyOrder);
        LimitOrder buyOrder = createLimitOrder(100.0, 8.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder, ExecutionStatus.PARTIALLY_MATCHED);
    }

    private LimitOrder createLimitOrder(Double price, Double qty, OrderSide buyOrSell, String ticker) {

        PriceInformation priceInformation = new PriceInformation(new BigDecimal(price), Currency.SEK);
        return LimitOrder.builder().priceInformation(priceInformation).
                quantity(new BigDecimal(qty)).side(buyOrSell).ticker(ticker).orderStatus(OrderStatus.OPEN).build();
    }

    private void assertOrderStatus(LimitOrder order, ExecutionStatus expectedStatus) {
        when(limitOrderRepository.save(eq(order))).thenReturn(order);
        ExecutionStatus statusAfterSecondOrder = orderBookService.processOrder(OrderDTOMapper.toDto(order));
        assertEquals(expectedStatus, statusAfterSecondOrder);
    }

}
