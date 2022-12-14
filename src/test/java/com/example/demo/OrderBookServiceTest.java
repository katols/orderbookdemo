package com.example.demo;

import com.example.model.db.Order;
import com.example.model.db.PriceInformation;
import com.example.model.domain.Currency;
import com.example.model.domain.OrderBook;
import com.example.model.domain.OrderSide;
import com.example.model.domain.OrderStatus;
import com.example.model.dto.LimitOrderDTO;
import com.example.model.dto.OrderDTOMapper;
import com.example.model.dto.OrderSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
public class OrderBookServiceTest {
    OrderBookService orderBookService;
    @MockBean
    OrderRepository orderRepository;

    private static final String TICKER_AAPL = "AAPL";

    @BeforeEach
    public void init() {
        orderBookService = new OrderBookService(orderRepository);
        orderBookService.registerOrderBook(new OrderBook(TICKER_AAPL));
    }

    //TODO: Test that get volume returns an aggregated value, for total and each side

    @Test
    public void testFullMatchWithTwoEqualOrders() throws OrderbookException {
        Order order = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(order, OrderStatus.OPEN);

        Order order2 = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order2, OrderStatus.CLOSED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalVolumeForPriceLevel(100.0).doubleValue());

    }

    private BigDecimal getTotalVolumeForPriceLevel(double price) {
        PriceInformation priceInformation = new PriceInformation();
        priceInformation.setPrice(new BigDecimal(price));
        priceInformation.setCurrency(Currency.SEK);
        return orderBookService.getTotalVolumeForPriceLevel(TICKER_AAPL, priceInformation);
    }

    @Test
    public void testFullMatchWithTwoBuyOrdersEqualToOneSellOrder() throws OrderbookException {
        Order buyOrder1 = TestUtil.createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, OrderStatus.OPEN);

        Order buyOrder2 = TestUtil.createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, OrderStatus.OPEN);

        Order sellOrder = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, OrderStatus.CLOSED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalVolumeForPriceLevel(100.0).doubleValue());
    }

    @Test
    public void testFullMatchWithSandwich() throws OrderbookException {
        Order expectedOrder = TestUtil.createLimitOrder(100.0, 5.0, OrderSide.SELL, TICKER_AAPL);

        Order buyOrder1 = TestUtil.createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, OrderStatus.OPEN);

        Order sellOrder = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        when(orderRepository.save(eq(expectedOrder))).thenReturn(expectedOrder);
        assertOrderStatus(sellOrder, OrderStatus.PARTIALLY_MATCHED);

        Order buyOrder2 = TestUtil.createLimitOrder(100.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, OrderStatus.CLOSED);
        assertEquals(new BigDecimal(0).doubleValue(), getTotalVolumeForPriceLevel(100.0).doubleValue());
    }

    @Test
    public void testPartialMatchOnDifferentPriceLevels() throws OrderbookException {
        Order expectedBuyOrder = TestUtil.createLimitOrder(110.0, 30.0, OrderSide.BUY, TICKER_AAPL);

        Order order1 = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order1, OrderStatus.OPEN);

        Order order2 = TestUtil.createLimitOrder(110.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order2, OrderStatus.OPEN);

        Order order3 = TestUtil.createLimitOrder(120.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(order3, OrderStatus.OPEN);

        Order order4 = TestUtil.createLimitOrder(110.0, 50.0, OrderSide.BUY, TICKER_AAPL);
        when(orderRepository.save(eq(expectedBuyOrder))).thenReturn(expectedBuyOrder);
        assertOrderStatus(order4, OrderStatus.PARTIALLY_MATCHED);
        assertEquals(new BigDecimal("30.0").doubleValue(), getTotalVolumeForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal("10.0").doubleValue(), getTotalVolumeForPriceLevel(120).doubleValue());

    }

    @Test
    public void bestDealAtBuyTest() throws OrderbookException {
        Order sellOrder1 = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder1, OrderStatus.OPEN);

        Order sellOrder2 = TestUtil.createLimitOrder(110.0, 5.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder2, OrderStatus.OPEN);

        Order sellOrder3 = TestUtil.createLimitOrder(120.0, 15.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder3, OrderStatus.OPEN);

        Order buyOrder = TestUtil.createLimitOrder(130.0, 14.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder, OrderStatus.CLOSED);

        assertEquals(new BigDecimal(0).doubleValue(), getTotalVolumeForPriceLevel(100).doubleValue());
        assertEquals(new BigDecimal(1).doubleValue(), getTotalVolumeForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(15).doubleValue(), getTotalVolumeForPriceLevel(120).doubleValue());
    }

    @Test
    public void bestDealAtSellTest() throws OrderbookException {
        Order buyOrder1 = TestUtil.createLimitOrder(100.0, 10.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder1, OrderStatus.OPEN);

        Order buyOrder2 = TestUtil.createLimitOrder(110.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder2, OrderStatus.OPEN);

        Order buyOrder3 = TestUtil.createLimitOrder(120.0, 15.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder3, OrderStatus.OPEN);

        Order sellOrder = TestUtil.createLimitOrder(100.0, 16.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, OrderStatus.CLOSED);

        assertEquals(new BigDecimal("10.0").doubleValue(), getTotalVolumeForPriceLevel(100).doubleValue());
        assertEquals(new BigDecimal("4.0").doubleValue(), getTotalVolumeForPriceLevel(110).doubleValue());
        assertEquals(new BigDecimal(0).doubleValue(), getTotalVolumeForPriceLevel(120).doubleValue());
    }


    @Test
    public void correctStatusTest() throws OrderbookException {
        Order expectedBuyOrder = TestUtil.createLimitOrder(100.0, 4.0, OrderSide.BUY, TICKER_AAPL);
        Order sellOrder = TestUtil.createLimitOrder(100.0, 4.0, OrderSide.SELL, TICKER_AAPL);
        assertOrderStatus(sellOrder, OrderStatus.OPEN);

        when(orderRepository.save(eq(expectedBuyOrder))).thenReturn(expectedBuyOrder);
        Order buyOrder = TestUtil.createLimitOrder(100.0, 8.0, OrderSide.BUY, TICKER_AAPL);
        assertOrderStatus(buyOrder, OrderStatus.PARTIALLY_MATCHED);
    }

    @Test
    public void calculateOrderStatisticsTest() {
        Order buyOrder1 = TestUtil.createLimitOrder(100.0, 25.0, OrderSide.BUY, TICKER_AAPL);
        Order buyOrder2 = TestUtil.createLimitOrder(200.0, 20.0, OrderSide.BUY, TICKER_AAPL);
        Order buyOrder3 = TestUtil.createLimitOrder(300.0, 5.0, OrderSide.BUY, TICKER_AAPL);
        when(orderRepository.search(eq(TICKER_AAPL))).thenReturn(Arrays.asList(buyOrder1, buyOrder2, buyOrder3));
        OrderSummaryDTO orderSummaryByDate = orderBookService.getOrderSummaryByDate(TICKER_AAPL, LocalDate.now(), OrderSide.BUY);
        assertEquals(new BigDecimal(160), orderSummaryByDate.getAverage());
    }

    private void assertOrderStatus(Order order, OrderStatus expectedStatus) throws OrderbookException {
        when(orderRepository.save(eq(order))).thenReturn(order);
        LimitOrderDTO statusAfterSecondOrder;
        statusAfterSecondOrder = orderBookService.processOrder(OrderDTOMapper.toDto(order));
        assertEquals(expectedStatus, statusAfterSecondOrder.getOrderStatus());
    }

}
