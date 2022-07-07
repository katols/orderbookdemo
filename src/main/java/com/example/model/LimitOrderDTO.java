package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class LimitOrderDTO {
    private Long id;
    private PriceInformationDTO priceInformation;
    private BigDecimal quantity;
    private OrderSide side;
    private String ticker;
    private Currency currency;
    private LocalDate creationTime;
    private OrderStatus orderStatus;
    private BigDecimal volume;
}
