package com.example.demo;

import com.example.model.db.Order;
import com.example.model.domain.OrderSide;
import com.example.model.dto.OrderDTOMapper;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;


import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderBookRestControllerTest {

    @LocalServerPort
    private int port;


    @Test
    public void testCreateOrder() {
        Order order = TestUtil.createLimitOrder(120.0, 30.0, OrderSide.SELL, "AAPL");
        given().port(port)
                .with()
                .header(new Header("Content-Type", "application/json"))
                .body(OrderDTOMapper.toDto(order)).when()
                .request("POST", "/api/orders/create")
                .then()
                .statusCode(200);

    }

    @Test
    public void testCreateAndGetOrderbyId() {
        Order order = TestUtil.createLimitOrder(120.0, 30.0, OrderSide.SELL, "AAPL");
        Response response = given().port(port)
                .with()
                .header(new Header("Content-Type", "application/json"))
                .body(OrderDTOMapper.toDto(order)).when()
                .request("POST", "/api/orders/create");

        JsonPath jsonEvaluator = response.jsonPath();
        assertThat(jsonEvaluator.get("quantity"), equalTo(30));
        String orderId = jsonEvaluator.get("id").toString();


        Response response1 = given().port(port).get("/api/orders/" + orderId);
        JsonPath jsonEvaluatorGetId = response1.jsonPath();
        assertThat(jsonEvaluatorGetId.get("id").toString(), equalTo(orderId));
        assertThat(jsonEvaluatorGetId.get("quantity"), equalTo(30.0f));

    }

    @Test
    public void testCreateOrderAndGetStatistics() {
        Order order1 = TestUtil.createLimitOrder(100.0, 20.0, OrderSide.SELL, "AAPL");
        Order order2 = TestUtil.createLimitOrder(200.0, 30.0, OrderSide.SELL, "AAPL");
        given().port(port)
                .with()
                .header(new Header("Content-Type", "application/json"))
                .body(OrderDTOMapper.toDto(order1)).when()
                .request("POST", "/api/orders/create")

                .then()
                .statusCode(200);
        given().port(port)
                .with()
                .header(new Header("Content-Type", "application/json"))
                .body(OrderDTOMapper.toDto(order2)).when()
                .request("POST", "/api/orders/create")

                .then()
                .statusCode(200);

        String dateString = LocalDate.now().toString();
        Response response = given().port(port).get("/api/orders/ordersummary?ticker=AAPL&date=" + dateString + "&side=SELL");
        JsonPath jsonEvaluator = response.jsonPath();

        assertThat(jsonEvaluator.get("min"), equalTo(100.0f));
        assertThat(jsonEvaluator.get("max"), equalTo(200.0f));
        assertThat(jsonEvaluator.get("average"), equalTo(160.0f));
        assertThat(jsonEvaluator.get("noOfOrders"), equalTo(2));

    }
}
