FROM openjdk:8-jdk

ENV SCALA_VERSION 2.12.6
ENV SBT_VERSION 1.1.4

# Install sbt
RUN curl -L -o "sbt-${SBT_VERSION}.deb" "http://dl.bintray.com/sbt/debian/sbt-${SBT_VERSION}.deb"
RUN dpkg -i "sbt-${SBT_VERSION}.deb"
RUN rm "sbt-${SBT_VERSION}.deb"
RUN apt-get update
RUN apt-get install -y sbt

# Install node
RUN curl -sL https://deb.nodesource.com/setup_6.x | bash -
RUN apt-get install -y nodejs

# Configure entrypoint
WORKDIR /usr/src/snakes
COPY ./build.sh /usr/src/snakes/build.sh
ENTRYPOINT ["./build.sh"]

