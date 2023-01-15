# cheap-spot

## Run locally

```sh
lein run
curl localhost:8080
```

## Build and run jar

```sh
lein uberjar
java -jar target/cheap-spot.jar
```

## Build and run in docker locally

```sh
docker build -t cheap-spot .
docker run -it --rm --name cheap-spot-app -p 8080:8080 cheap-spot
```

## Deploy to fly.io

```sh
lein clean
fly deploy
```