# Orderbook Service

## General

This application runs on: 

* Spring Boot 2.7.1
* Java JDK 12.0.1
* Docker 20.10.17
* MySQL 8.0.29

## Setup

This application can be run both with docker only and by starting the main class of the Spring Boot application.
In the latter case MySQL needs to be setup separately (more details [here](https://hub.docker.com/_/mysql) ) 
or alternatively you can also run the Spring Boot application directly against the MySQL container that is 
started with the below docker commands, a useful approach when e.g. debugging the application from an IDE. 

The following docker commands will launch a docker container with the database MySQL and another container 
with the Spring Boot application. 

To view the application, start your docker desktop application and run the following in your terminal window 
in the root directory of the downloaded code:

`docker-compose build`

`docker-compose up`

The web server and its REST api can be accessed in 2 different ways.
When starting with docker: 
http://localhost:6868/swagger-ui/index.html

When starting locally via IDE: 
http://localhost:8081/swagger-ui/index.html

## About
This application provides a basic REST API and the functionality of a simple matching engine. 
The matching is automatic, which means that when an order is created successfully, 
it is instantly executed against any present orders already existing on an orderbook. 
An orderbook is identified by a ticker with a character length of maximum 5. 
Currently the application supports 4 different orderbooks: AAPL, SAVE, GME and TSLA.
All successfully created orders are saved in a single table in a MySQL database and an order summary
(including max/min/average price) can be retrieved for each ticker, date and side of orderbook. An order can also be retrieved using the 
id created by the application. Orders can be cancelled but will never be deleted by the application. 

## Some Important Limitations
* Orderbooks cannot be added or deleted in runtime. It is not possible to enter orders for other orderbooks 
than the supported ones. 
* The application needs to be restarted every day to function correctly. Only today's orders will be traded.
* Restarting in the middle of the trading day is not supported (todays orders will need manual removal).
* The application only handles Limit Orders. Future support for Market orders may be considered. 
