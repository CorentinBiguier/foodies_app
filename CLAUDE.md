# Foodies App

Projet mono-repo de micro-services autour de la cuisine (utilisateurs, recettes,
inventaire, scraping). Objectif double : construire l'app **et** monter en
compétence sur l'usage de Claude Code (skills, sous-agents, prompting multi-étapes).

Voir la roadmap complète par paliers (non versionnée ici, cf. conversation /
notes du projet) pour le détail fonctionnel et pédagogique de chaque étape.

## État actuel

Palier 3 en cours. `user-service` (Postgres + Liquibase, Swagger, auth JWT
basique) et `recipe-service` (CRUD recette, lien REST vers `user-service`
avec timeout/retry) existent ; `services/_template` sert de gabarit pour les
suivants.

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
  config/       # beans de configuration Spring (ex. OpenAPI/Swagger)
  client/       # client HTTP vers un autre micro-service (optionnel, voir
                # "Communication inter-services" — tous les services n'en
                # ont pas besoin)
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
- Documentation API : springdoc-openapi expose une UI Swagger sur
  `/swagger-ui.html` (redirige vers `/swagger-ui/index.html`) et le JSON
  OpenAPI sur `/v3/api-docs`, pour chaque service (établi dans `_template`,
  présent par défaut dans tout nouveau service scaffoldé). Le titre affiché
  correspond à `spring.application.name` (`config/OpenApiConfig.java`), pas
  de valeur codée en dur à maintenir manuellement.
- Authentification : JWT stateless via Spring Security
  (`spring-boot-starter-security` + `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/
  `jjwt-jackson`), établi avec `user-service`, à répliquer pour tout service
  suivant qui expose une API protégée :
  - `POST /api/auth/register` et `POST /api/auth/login` (`AuthController` /
    `AuthService`) émettent un token ; toutes les autres routes sont
    protégées par défaut (`config/SecurityConfig.java`), sauf
    `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`.
  - `config/JwtAuthenticationFilter.java` lit l'en-tête
    `Authorization: Bearer <token>`, valide la signature via `JwtService`
    (`service/`) et peuple manuellement le `SecurityContextHolder` — pas de
    `UserDetailsService`/`AuthenticationManager`, volontairement minimal
    (pas de rôles/permissions pour l'instant, juste authentifié/non
    authentifié). `UserDetailsServiceAutoConfiguration` exclue dans
    `@SpringBootApplication` pour ne pas générer un utilisateur/mot de passe
    par défaut inutilisé.
  - Mots de passe hashés avec `BCryptPasswordEncoder`, jamais stockés ni
    loggés en clair.
  - Expiration du token configurable via `JWT_EXPIRATION_MS`
    (`0` = pas d'expiration, valeur par défaut actuelle) ; secret via
    `JWT_SECRET` (chaîne Base64 aléatoire ≥32 octets, générée à la main,
    jamais commitée, pas de valeur par défaut dans `application.yml` —
    l'appli refuse de démarrer si absente).
  - Erreurs 401/409 suivent le même format `{error, details}` via
    `GlobalExceptionHandler` que le reste de l'API.
  - Les tests `@SpringBootTest` ont besoin d'un `jwt.secret` (pas de défaut
    en prod) : `src/test/resources/application.yml` fournit une valeur de
    test dédiée, jamais réutilisée hors tests.
- Communication inter-services : établi avec `recipe-service` appelant
  `user-service` (résolution de l'auteur d'une recette), à répliquer pour
  tout futur appel service-à-service :
  - Client HTTP synchrone via `RestClient` (pas `RestTemplate`, en fin de
    vie, ni `WebClient`, pile réactive superflue pour un service Spring MVC
    classique), package `client/` (`XxxClient` interface + `client/impl/`).
  - Timeout : `JdkClientHttpRequestFactory` (`java.net.http.HttpClient` du
    JDK), connect ET read timeout en `Duration`, jamais un cast en millis.
  - Retry : `spring-retry` + `spring-boot-starter-aop` (indispensable —
    `@Retryable` est tissé par proxy AOP, silencieusement no-op sans cette
    dépendance), `@EnableRetry` sur la classe `@SpringBootApplication`.
    Retry uniquement sur les erreurs de connectivité/5xx transitoires
    (`ResourceAccessException`, `HttpServerErrorException`) — **jamais** sur
    un 401 : un mauvais token ne devient pas valide en réessayant. Dès
    qu'une méthode a au moins un `@Recover`, Spring Retry route **toute**
    exception qu'elle lève vers la recherche d'une méthode `@Recover`
    correspondante, même hors de `retryFor` — prévoir un `@Recover`
    générique (`RuntimeException`) qui relance tel quel, sinon les
    exceptions non retryable ressortent en `ExhaustedRetryException`
    trompeur au lieu de se propager normalement.
  - Config externalisée en `@Value` (même style que `jwt.*`) :
    `<service-appelé>.base-url`/`timeout-ms`/`retry.max-attempts`, avec
    variables d'env dédiées (`USER_SERVICE_TIMEOUT_MS`,
    `USER_SERVICE_RETRY_MAX_ATTEMPTS`) ; l'URL elle-même est codée en dur
    dans `docker-compose.yml` (nom du conteneur + port interne, ex.
    `http://user-service:8080`) plutôt que mise en `.env`, ce n'est ni un
    secret ni une valeur qui varie par environnement compose.
  - Règle de conception : **dénormaliser à la création, ne jamais
    re-résoudre à la lecture**. Une référence vers une autre entité de
    service (ex. auteur d'une recette) se snapshot une fois (id + nom) au
    moment de l'écriture, jamais re-fetchée sur les lectures — sinon les
    lectures deviennent dépendantes de la disponibilité d'un autre service.
    Compromis assumé : l'info dénormalisée peut devenir périmée (ex. un nom
    d'utilisateur changé après coup) jusqu'à la prochaine écriture qui
    déclenche un nouvel appel.
- Tests d'intégration avec Testcontainers pour tout ce qui touche la DB, pas de H2
- Commits : `type(scope): message` (ex: `feat(recipe-service): add ingredient validation`)

## Ne jamais
- Committer un `.env`
- Toucher aux changelogs Liquibase déjà présents dans `src/main/resources/db/changelog` (créer un nouveau changeset numéroté à la place)
- Committer un vrai `JWT_SECRET`, ou logguer un mot de passe en clair

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
