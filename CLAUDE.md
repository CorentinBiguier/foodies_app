# Foodies App

Projet mono-repo de micro-services autour de la cuisine (utilisateurs, recettes,
inventaire, scraping). Objectif double : construire l'app **et** monter en
compétence sur l'usage de Claude Code (skills, sous-agents, prompting multi-étapes).

Voir la roadmap complète par paliers (non versionnée ici, cf. conversation /
notes du projet) pour le détail fonctionnel et pédagogique de chaque étape.

## État actuel

Palier 0 (setup) en cours. Aucun service n'existe encore.

## Structure cible du mono-repo

```
services/
  _template/       # gabarit Spring Boot pour tout nouveau service
  user-service/
  recipe-service/
  inventory-service/
  scraper-service/
  gateway/
docker-compose.yml
```

Chaque service est un module Maven indépendant avec son propre `Dockerfile` et
son propre healthcheck. Un `CLAUDE.md` pourra être ajouté par service quand le
mono-repo grossira (prévu au Palier 1).

## Stack technique

- Java 21 / Spring Boot 3 pour tous les services
- Maven comme outil de build (un `pom.xml` par service)
- Docker + Docker Compose pour l'orchestration locale
- Communication inter-services en REST (pas de message broker prévu pour l'instant)

## Conventions

- Pas encore de convention figée : à documenter au fur et à mesure que
  `services/_template` et le premier service (`user-service`) prennent forme.
- Mettre à jour cette section dès que des règles de nommage, structure de
  package ou style de code se stabilisent.

## Notes de travail avec Claude Code

- Utiliser le mode plan pour toute modification structurante (nouveau
  service, changement de schéma, docker-compose) ; acceptEdits convient pour
  des itérations rapides et réversibles (un fichier, un test).
