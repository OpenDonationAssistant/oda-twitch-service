FROM fedora:41
WORKDIR /app
COPY target/oda-twitch-service /app

CMD ["./oda-twitch-service"]
