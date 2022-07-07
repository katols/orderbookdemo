package com.example.model;

import java.util.Comparator;

public class PriceInformationComparator implements Comparator<PriceInformation> {
    @Override
    public int compare(PriceInformation o1, PriceInformation o2) {
        return o1.getPrice().compareTo(o2.getPrice());
    }
}
