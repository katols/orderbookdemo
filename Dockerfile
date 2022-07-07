FROM maven:3.8.6-jdk-11
WORKDIR ~/Downloads/demo
COPY . .
RUN mvn clean install
CMD mvn spring-boot:run