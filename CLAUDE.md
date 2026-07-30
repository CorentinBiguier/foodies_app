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

Architecture de package par service (établie dans `services/_template`,
à répliquer telle quelle pour chaque nouveau service) :

```
com.foodies.<service>/
  controller/   # REST controllers + @RestControllerAdvice pour les erreurs
  service/      # interfaces métier
  service/impl/ # implémentations (XxxServiceImpl)
  modele/       # DTOs, sous forme de record Java (entrée/sortie API)
  entite/       # entités JPA
  repository/   # interfaces Spring Data JPA
```

- Le controller ne dépend que de l'interface `service`, jamais de l'impl ni
  du repository directement.
- Les DTOs (`modele`) sont des `record` immuables ; on distingue déjà requête
  de création (ex. `CreateXxxRequest`) et DTO de réponse (ex. `XxxDto`) quand
  ça a du sens.
- Persistance : H2 en mémoire pour `_template` (suffisant pour dev/tests) ;
  les services réels brancheront une base persistante dès qu'ils en auront
  besoin (à partir du Palier 2).

### Tests

- **Unitaires** (`*Test.java`, ex. `ExampleServiceImplTest`) : ciblent la
  couche `service`, avec Mockito pour mocker le repository. Exécutés par
  Surefire (`mvn test`), sans contexte Spring.
- **Intégration** (`*IT.java`, ex. `ExampleControllerIT`) : ciblent la couche
  `controller`, avec `@SpringBootTest` + `MockMvc` sur le contexte complet
  (vraie base H2, vrai service, vrai repository). Exécutés par Failsafe
  (`mvn verify`), pas par `mvn test` (convention de nommage Maven standard,
  aucune config d'exclusion nécessaire).

## Notes de travail avec Claude Code

- Utiliser le mode plan pour toute modification structurante (nouveau
  service, changement de schéma, docker-compose) ; acceptEdits convient pour
  des itérations rapides et réversibles (un fichier, un test).
