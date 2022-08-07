This application can be run both with docker compose and by starting the main class of the Spring Boot application. It relies on a MySQL database that needs to be setup separately. 

To view the application, start your docker desktop application and run the following in your terminal window:

docker-compose build
docker-compose up

The web server can be accessed in 2 different ways.
When starting with docker: 
http://localhost:6868/swagger-ui/index.html

When starting locally via intellij: 
http://localhost:8081/swagger-ui/index.html

