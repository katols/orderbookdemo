package com.example.model.domain;

public enum OrderSide {
    BUY, SELL;

    public boolean isBuy(){
        return this == BUY;
    }


}
