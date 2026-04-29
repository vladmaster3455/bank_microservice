# Bank Microservices

Plateforme bancaire basée sur une architecture microservices avec Spring Boot, Spring Cloud, Eureka, API Gateway, PostgreSQL, Kafka et Keycloak.

## Objectif

Ce projet démontre un flux bancaire simple :
- gestion des comptes
- exécution de virements
- publication asynchrone d'événements de notification
- sécurisation des APIs via OAuth2/JWT (Keycloak)

## Architecture

Modules Maven :
- `discovery` : service registry Eureka
- `gateway` : point d'entrée unique, routage + sécurité + rate limiting
- `account-service` : CRUD compte + opérations débit/crédit
- `transaction-service` : virement entre comptes + publication Kafka (outbox)
- `notification-service` : consommation des événements Kafka de notification

Composants d'infrastructure :
- PostgreSQL (un pour `account-service`, un pour `transaction-service`)
- Kafka + Zookeeper
- Keycloak (realm importé automatiquement)

## Stack technique

- Java 21
- Spring Boot 3.3.5
- Spring Cloud 2023.0.5
- Spring Cloud Gateway
- Spring Security OAuth2 Resource Server
- Spring Data JPA + Flyway
- Apache Kafka
- Docker Compose

## Ports et URLs utiles

- Gateway : `http://localhost:8080`
- Account service : `http://localhost:8081`
- Transaction service : `http://localhost:8082`
- Notification service : `http://localhost:8083`
- Eureka Dashboard : `http://localhost:8761`
- Keycloak : `http://localhost:9080`
- Kafka local (host) : `localhost:29092`
- PostgreSQL account : `localhost:5433`
- PostgreSQL transaction : `localhost:5434`

## Démarrage rapide avec Docker

Depuis la racine du repository :

```bash
docker compose up --build -d
```

Vérifier les services :

```bash
docker compose ps
```

Arrêter :

```bash
docker compose down
```

Supprimer aussi les volumes :

```bash
docker compose down -v
```

## Authentification Keycloak

Le fichier `keycloak/realm-export.json` crée automatiquement :
- realm : `bank-realm`
- client public : `bank-gateway-client`
- utilisateur USER : `bankuser / bankpass`
- utilisateur ADMIN : `bankadmin / bankpass`

Récupérer un token (exemple USER) :

```bash
TOKEN=$(curl -s -X POST "http://localhost:9080/realms/bank-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=bank-gateway-client" \
  -d "username=bankuser" \
  -d "password=bankpass" | jq -r '.access_token')

echo "$TOKEN"
```

## Endpoints principaux (via Gateway)

Base URL : `http://localhost:8080`

Comptes (`/api/accounts`)
- `POST /api/accounts` (ADMIN)
- `GET /api/accounts` (USER ou ADMIN)
- `GET /api/accounts/{id}` (USER ou ADMIN)
- `PUT /api/accounts/{id}` (ADMIN)
- `DELETE /api/accounts/{id}` (ADMIN)
- `POST /api/accounts/{id}/debit` (ADMIN)
- `POST /api/accounts/{id}/credit` (ADMIN)

Virements (`/api/transfers`)
- `POST /api/transfers` (USER ou ADMIN)
- `GET /api/transfers` (USER ou ADMIN)
- `GET /api/transfers/{id}` (USER ou ADMIN)

### Exemples d'appels

Créer un compte :

```bash
curl -X POST "http://localhost:8080/api/accounts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC-0001",
    "ownerName": "Alice Martin",
    "initialBalance": 1000.00,
    "currency": "EUR",
    "status": "ACTIVE"
  }'
```

Effectuer un virement :

```bash
curl -X POST "http://localhost:8080/api/transfers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "11111111-1111-1111-1111-111111111111",
    "targetAccountId": "22222222-2222-2222-2222-222222222222",
    "amount": 10.50,
    "currency": "EUR",
    "idempotencyKey": "transfer-001"
  }'
```

## Démarrage en local (sans Docker pour les apps)

Pré-requis :
- Java 21
- Maven 3.9+
- PostgreSQL, Kafka, Keycloak disponibles localement (ou via `docker compose`)

Compiler tout le projet :

```bash
mvn clean install
```

Lancer les services (ordre recommandé) :
1. `discovery`
2. `account-service`
3. `transaction-service`
4. `notification-service`
5. `gateway`

Exemple de lancement d'un module :

```bash
mvn -pl discovery spring-boot:run
```

## Observabilité

Chaque service expose Actuator :
- `/actuator/health`
- `/actuator/info`

Gateway expose aussi :
- `/actuator/gateway`

## Notes

- Les schémas de base sont versionnés via Flyway (`db/migration`).
- Le `transaction-service` publie des événements sur le topic Kafka `bank.notifications`.
- Le `notification-service` consomme ce topic avec le consumer group `notification-service`.
