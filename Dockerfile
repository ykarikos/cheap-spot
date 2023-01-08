FROM clojure
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps
RUN lein uberjar
COPY . /usr/src/app
CMD ["java", "-jar", "target/cheap-spot.jar"]