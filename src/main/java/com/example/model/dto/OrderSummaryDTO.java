package com.example.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderSummaryDTO {
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal average;
    private int noOfOrders;



}
