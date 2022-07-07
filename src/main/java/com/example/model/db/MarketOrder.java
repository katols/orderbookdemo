package com.example.model.db;

import com.example.model.domain.ExecutionAction;
import com.example.model.interfaces.IOrder;
import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@Entity
@DiscriminatorValue("market")
public class MarketOrder extends Order {

    @Override
    public Map<ExecutionAction, List<IOrder>> matchAgainstExistingOrders(IOrder order, TreeMap<PriceInformation, LinkedList<IOrder>> orders) {
        return null;
    }
}
