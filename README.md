# ODA Twitch Service
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/OpenDonationAssistant/oda-twitch-service)
![Sonar Tech Debt](https://img.shields.io/sonar/tech_debt/OpenDonationAssistant_oda-twitch-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Violations](https://img.shields.io/sonar/violations/OpenDonationAssistant_oda-twitch-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Tests](https://img.shields.io/sonar/tests/OpenDonationAssistant_oda-twitch-service?server=https%3A%2F%2Fsonarcloud.io)
![Sonar Coverage](https://img.shields.io/sonar/coverage/OpenDonationAssistant_oda-twitch-service?server=https%3A%2F%2Fsonarcloud.io)

## Running with Docker

The service is published to GitHub Container Registry.

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `TWITCH_CLIENT_ID` | Twitch API client ID | (required) |
| `TWITCH_CLIENT_SECRET` | Twitch API client secret | (required) |
| `JDBC_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost/postgres?currentSchema=automation` |
| `JDBC_USER` | PostgreSQL username | `postgres` |
| `JDBC_PASSWORD` | PostgreSQL password | `postgres` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |

### Docker Run Example

```bash
docker run -d \
  --name oda-twitch-service \
  -p 8080:8080 \
  -e TWITCH_CLIENT_ID=your_client_id \
  -e TWITCH_CLIENT_SECRET=your_client_secret \
  -e JDBC_URL=jdbc:postgresql://postgres:5432/automation \
  -e JDBC_USER=postgres \
  -e JDBC_PASSWORD=your_password \
  -e RABBITMQ_HOST=rabbitmq \
  ghcr.io/opendonationassistant/oda-twitch-service:latest
```

### Docker Compose Example

```yaml
services:
  oda-twitch-service:
    image: ghcr.io/opendonationassistant/oda-twitch-service:latest
    ports:
      - "8080:8080"
    environment:
      - TWITCH_CLIENT_ID=your_client_id
      - TWITCH_CLIENT_SECRET=your_client_secret
      - JDBC_URL=jdbc:postgresql://postgres:5432/automation
      - JDBC_USER=postgres
      - JDBC_PASSWORD=your_password
      - RABBITMQ_HOST=rabbitmq
    depends_on:
      - postgres
      - rabbitmq

  postgres:
    image: postgres:16
    environment:
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=your_password
      - POSTGRES_DB=automation
    volumes:
      - postgres_data:/var/lib/postgresql/data

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "15672:15672"

volumes:
  postgres_data:
```

