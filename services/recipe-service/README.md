# recipe-service

Port hôte (docker-compose) : **8082** (conteneur écoute en interne sur 8080).

## Endpoints

> Générés depuis le template, à vérifier/compléter au fur et à mesure de
> l'implémentation réelle du domaine Recipe.

- `GET /api/recipes`
- `GET /api/recipes/{id}`
- `POST /api/recipes`
- `GET /actuator/health`

## Lancer localement

```bash
docker compose build recipe-service
docker compose up -d recipe-service
```
