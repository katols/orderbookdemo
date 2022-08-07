This application can be run both with docker compose and by starting the main class of the Spring Boot application. It relies on a MySQL database that needs to be setup separately. 

To setup MySQL, run the following in your terminal window:

`$> docker network create db-net`
`$> docker run -it --network db-net --rm mysql mysql -horderbookdb -uroot -p`
`$> docker network connect db-net orderbookdb`

