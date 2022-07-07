package com.example.demo;

import com.example.model.db.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select s from Order s where s.ticker like :ticker%")
    List<Order> search(String ticker);

}
