package com.example.model.dto;


import com.example.model.db.LimitOrder;
import com.example.model.db.Order;
import com.example.model.db.PriceInformation;

import java.sql.Timestamp;
import java.time.LocalDate;

public class OrderDTOMapper {
    public static PriceInformationDTO toDto(PriceInformation priceInformation) {
        return new PriceInformationDTO(priceInformation.getPrice(), priceInformation.getCurrency());
    }

    public static LimitOrderDTO toDto(Order order) {
        return LimitOrderDTO.builder().
                id(order.getId()).
                priceInformation(toDto(order.getPriceInformation())).
                quantity(order.getQuantity()).
                side(order.getSide()).
                ticker(order.getTicker()).
                creationTime(toLocalDate(order.getCreationTime())).
                orderStatus(order.getOrderStatus()).
                volume(order.getVolume()).
                build();
    }

    public static PriceInformation fromDto(PriceInformationDTO priceInformationDTO) {
        return new PriceInformation(priceInformationDTO.getPrice(), priceInformationDTO.getCurrency());
    }

    public static LimitOrder fromDto(LimitOrderDTO limitOrderDTO) {

        return new LimitOrder(
                fromDto(limitOrderDTO.getPriceInformation()),
                limitOrderDTO.getQuantity(),
                limitOrderDTO.getSide(),
                limitOrderDTO.getTicker()
        );
    }

    private static LocalDate toLocalDate(Timestamp creationTime) {
        return (creationTime != null) ? creationTime.toLocalDateTime().toLocalDate() : LocalDate.now();
    }

}
