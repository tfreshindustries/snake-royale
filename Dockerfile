FROM openjdk:slim

COPY server/target/scala-2.12/server.jar /usr/src/snakes/snakes.jar
COPY frontend/build                      /usr/src/snakes/frontend/build

WORKDIR /usr/src/snakes

CMD ["java", "-jar", "snakes.jar"]
