package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PriceInformationDTO {
    private BigDecimal price;
    private Currency currency;
}
