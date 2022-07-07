package com.example.model;

public enum OrderSide {
    BUY, SELL;

    public boolean isBuy(){
        return this == BUY;
    }


}
