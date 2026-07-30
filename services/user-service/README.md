# user-service

Port hôte (docker-compose) : **8081** (conteneur écoute en interne sur 8080).

## Endpoints

> Générés depuis le template, à vérifier/compléter au fur et à mesure de
> l'implémentation réelle du domaine User.

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `GET /actuator/health`

## Lancer localement

```bash
docker compose build user-service
docker compose up -d user-service
```
