FROM fedora:44
WORKDIR /app
COPY target/oda-twitch-service /app

CMD ["./oda-twitch-service"]
