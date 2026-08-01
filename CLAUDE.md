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
pom.xml             # agrégateur Maven (voir ci-dessous)
```

Chaque service est un module Maven indépendant avec son propre `Dockerfile` et
son propre healthcheck. Un `CLAUDE.md` pourra être ajouté par service quand le
mono-repo grossira (prévu au Palier 1).

Le `pom.xml` à la racine du repo est un agrégateur pur (`packaging: pom` +
`<modules>`), pas le parent Maven des services — chaque service garde
`spring-boot-starter-parent` comme parent et reste indépendant en
build/déploiement. Son seul rôle est de permettre à l'IDE (et à un
`mvn validate` lancé depuis la racine) de découvrir tous les services
automatiquement, sans avoir à importer chaque `pom.xml` de service à la main.
Le skill `new-microservice` y ajoute automatiquement le nouveau `<module>` à
chaque scaffold — ne jamais y ajouter de `dependencyManagement` ou de config
partagée, ça casserait l'indépendance des services.

## Stack technique

- Java 21 / Spring Boot 3 pour tous les services
- Maven comme outil de build (un `pom.xml` par service)
- Docker + Docker Compose pour l'orchestration locale
- Communication inter-services en REST (pas de message broker prévu pour l'instant)
- BDD Postgres 17 + Liquibase (changelogs YAML) pour les migrations de schéma
- Une seule instance Postgres partagée (service `postgres` dans
  `docker-compose.yml`), avec une base dédiée par service (ex.
  `user_service_db`). Isolation logique, pas physique — plus léger en local
  qu'un conteneur Postgres par service.
  Pour ajouter la base d'un nouveau service : compléter
  `infra/postgres/init/01-init-databases.sql` (suit le même modèle
  idempotent que l'entrée existante). Ce script ne s'exécute qu'au tout
  premier démarrage du conteneur (volume vide) — si Postgres tourne déjà,
  exécuter la commande `CREATE DATABASE` manuellement via
  `docker compose exec postgres psql -U foodies -c "..."`.

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
- Persistance : H2 en mémoire pour `_template` (suffisant pour dev/tests,
  scaffolding rapide) ; les services réels utilisent Postgres + Liquibase dès
  qu'ils gèrent des données persistantes (établi avec `user-service`, à
  répliquer pour les suivants) :
  - `pom.xml` : driver `org.postgresql:postgresql` (runtime) +
    `org.liquibase:liquibase-core`, pas de dépendance H2.
  - `application.yml` : datasource via variables d'env
    (`DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`, avec des valeurs
    par défaut `localhost`/`5432` pour un lancement hors docker-compose),
    `spring.jpa.hibernate.ddl-auto: validate` (Liquibase possède le schéma,
    Hibernate vérifie juste la cohérence), `spring.liquibase.change-log`
    pointant vers `classpath:db/changelog/db.changelog-master.yaml`.
  - Changelogs Liquibase en YAML dans
    `src/main/resources/db/changelog/`, un fichier par changeset numéroté
    (`001-xxx.yaml`, `002-xxx.yaml`...), inclus depuis le master.
- API REST : erreurs au format `{ "error": string, "details": string }`, codes HTTP standards (400 validation, 404 introuvable, 409 conflit)
- Tests d'intégration avec Testcontainers pour tout ce qui touche la DB, pas de H2
- Commits : `type(scope): message` (ex: `feat(recipe-service): add ingredient validation`)

## Ne jamais
- Committer un `.env`
- Toucher aux changelogs Liquibase déjà présents dans `src/main/resources/db/changelog` (créer un nouveau changeset numéroté à la place)

### Tests

- **Unitaires** (`*Test.java`, ex. `ExampleServiceImplTest`) : ciblent la
  couche `service`, avec Mockito pour mocker le repository. Exécutés par
  Surefire (`mvn test`), sans contexte Spring.
- **Intégration** (`*IT.java`, ex. `ExampleControllerIT`) : ciblent la couche
  `controller`, avec `@SpringBootTest` + `MockMvc` sur le contexte complet
  (vrai service, vrai repository). Exécutés par Failsafe (`mvn verify`), pas
  par `mvn test` (convention de nommage Maven standard, aucune config
  d'exclusion nécessaire).
  - Dans `_template` : base H2 réelle (suffisant, pas de Postgres à gérer).
  - Dans les services réels (ex. `user-service`) : vrai Postgres éphémère via
    Testcontainers, pas H2 — voir `TestcontainersConfig` (classe
    `@TestConfiguration` avec un bean `@ServiceConnection
    PostgreSQLContainer`, importée via `@Import(TestcontainersConfig.class)`
    dans chaque classe de test qui a besoin du contexte Spring complet,
    y compris le smoke test `*ApplicationTests`). Nécessite un Docker
    fonctionnel pour la JVM qui lance les tests (voir note ci-dessous).

## Notes de travail avec Claude Code

- Utiliser le mode plan pour toute modification structurante (nouveau
  service, changement de schéma, docker-compose) ; acceptEdits convient pour
  des itérations rapides et réversibles (un fichier, un test).
