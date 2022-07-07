package com.example.demo;

import com.example.model.IOrder;
import com.example.model.LimitOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LimitOrderRepository extends JpaRepository<IOrder, Long> {

    @Query("select s from LimitOrder s where s.ticker like :ticker%")
    List<LimitOrder> search(String ticker);

}
