
This application can be run both with docker only and by starting the main class of the Spring Boot application.
In the latter case MySQL needs to be setup separately (more details [here](https://hub.docker.com/_/mysql) ) 
or alternatively you can also run the Spring Boot application directly against the MySQL container that is 
started with the below docker commands, a useful approach when e.g. debugging the application from an IDE. 

The following docker commands will launch a docker container with the database MySQL and another container 
with the Spring Boot application. 

To view the application, start your docker desktop application and run the following in your terminal window:

`docker-compose build`

`docker-compose up`

The web server can be accessed in 2 different ways.
When starting with docker: 
http://localhost:6868/swagger-ui/index.html

When starting locally via IDE: 
http://localhost:8081/swagger-ui/index.html

