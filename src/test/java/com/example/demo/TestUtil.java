package com.example.demo;

import com.example.model.db.LimitOrder;
import com.example.model.db.Order;
import com.example.model.db.PriceInformation;
import com.example.model.domain.Currency;
import com.example.model.domain.OrderSide;
import com.example.model.domain.OrderStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TestUtil {
    public static Order createLimitOrder(Double price, Double qty, OrderSide buyOrSell, String ticker) {

        PriceInformation priceInformation = new PriceInformation(new BigDecimal(price), Currency.SEK);
        LimitOrder limitOrder = new LimitOrder(priceInformation, new BigDecimal(qty), buyOrSell, ticker, OrderStatus.OPEN);
        limitOrder.setCreationTime(Timestamp.valueOf(LocalDateTime.now()));
        return limitOrder;
    }
}
