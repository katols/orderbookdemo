package com.example.model.db;

import com.example.model.domain.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "price_information")
public class PriceInformation implements Comparable<PriceInformation> {
    @Id
    @GeneratedValue
    private Long id;
    private BigDecimal price;
    private Currency currency;

    public PriceInformation(BigDecimal price, Currency currency){
        this.price = price;
        this.currency = currency;
    }

    @Override
    public int compareTo(PriceInformation o) {
        return price.compareTo(o.getPrice());
    }


    @Override
    public boolean equals(Object compareTo) {
        if (compareTo == this) return true;
        if (!(compareTo instanceof PriceInformation)) {
            return false;
        }

        PriceInformation priceInformation = (PriceInformation) compareTo;
        return priceInformation.price.compareTo(this.price) == 0 &&
               priceInformation.currency == this.currency;

    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (int) price.intValue();
        result = prime * result + currency.ordinal();
        return result;
    }
    
}
