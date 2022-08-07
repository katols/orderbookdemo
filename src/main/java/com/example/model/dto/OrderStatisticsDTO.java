package com.example.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

//TODO: ensure immutability?
@Data
@AllArgsConstructor
public class OrderStatisticsDTO {
    private BigDecimal min;
    private BigDecimal max;
    private BigDecimal average;
    private int noOfOrders;



}
