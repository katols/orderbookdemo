package com.example.model;


import java.sql.Timestamp;
import java.time.LocalDate;

public class OrderDTOMapper {
    public static PriceInformationDTO toDto(PriceInformation priceInformation) {
        return new PriceInformationDTO(priceInformation.getPrice(), priceInformation.getCurrency());
    }

    public static LimitOrderDTO toDto(LimitOrder limitOrder) {
        return LimitOrderDTO.builder().
                id(limitOrder.getId()).
                priceInformation(toDto(limitOrder.getPriceInformation())).
                quantity(limitOrder.getQuantity()).
                side(limitOrder.getSide()).
                ticker(limitOrder.getTicker()).
                creationTime(toLocalDate(limitOrder.getCreationTime())).
                orderStatus(limitOrder.getOrderStatus()).
                volume(limitOrder.getVolume()).
                build();
    }

    public static PriceInformation fromDto(PriceInformationDTO priceInformationDTO) {
        return new PriceInformation(priceInformationDTO.getPrice(), priceInformationDTO.getCurrency());
    }

    public static LimitOrder fromDto(LimitOrderDTO limitOrderDTO) {
        return LimitOrder.builder().
                priceInformation(fromDto(limitOrderDTO.getPriceInformation())).
                quantity(limitOrderDTO.getQuantity()).
                side(limitOrderDTO.getSide()).
                ticker(limitOrderDTO.getTicker()).
                orderStatus(limitOrderDTO.getOrderStatus()).
                volume(limitOrderDTO.getVolume()).
                build();
    }

    private static LocalDate toLocalDate(Timestamp creationTime) {
        return (creationTime != null) ? creationTime.toLocalDateTime().toLocalDate() : LocalDate.now();
    }

}
