package com.example.model.dto;

import com.example.model.domain.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PriceInformationDTO {
    private BigDecimal price;
    private Currency currency;
}
