---
name: new-microservice
description: Scaffolder un nouveau microservice Spring Boot dans le mono-repo Foodies à partir de services/_template (structure Maven en couches, Dockerfile, healthcheck, entrée docker-compose, module reactor). Utiliser dès que l'utilisateur veut créer, ajouter ou démarrer un nouveau service (ex. user-service, recipe-service, inventory-service, scraper-service, ou tout autre nom en "xxx-service"), même s'il ne mentionne pas explicitement "scaffold" ou "template".
---

# Scaffolder un microservice

Ce skill duplique `services/_template` vers un nouveau service, en renommant le
package Java et en remplaçant le domaine de démo `Example` par le vrai domaine
du service (ex. `Example` → `User` pour `user-service`). Le résultat compile et
passe les tests immédiatement : c'est un point de départ fonctionnel à adapter,
pas un squelette vide.

La partie mécanique (copie, renommage de package/classes, entrée
docker-compose, port, module reactor) est déléguée à
`scripts/scaffold-service.sh` — ne la refais pas à la main, le script est
déterministe et évite les erreurs de renommage partiel. Le reste de ce fichier
couvre ce que le script ne peut pas décider à ta place.

## Étapes

1. **Déterminer le nom du service** (kebab-case, ex. `user-service`). Si
   l'utilisateur donne juste un domaine ("fais-moi le service utilisateurs"),
   propose le nom `<domaine>-service` avant de lancer le script.

2. **Lancer le script** depuis n'importe où dans le repo :
   ```bash
   bash .claude/skills/new-microservice/scripts/scaffold-service.sh <service-name>
   ```
   Il crée `services/<service-name>`, renomme le package
   `com.foodies.template` → `com.foodies.<domaine>`, renomme les classes
   `Template*`/`Example*` → `<Domaine>*`, met à jour `pom.xml` et
   `application.yml`, ajoute le service à `docker-compose.yml` (réseau
   `foodies-network` partagé, pas un réseau dédié) avec le prochain port hôte
   libre, l'enregistre comme `<module>` dans le `pom.xml` racine (agrégateur —
   voir plus bas), et génère un `README.md` de départ.

3. **Recharger Maven dans l'IDE.** Le `pom.xml` racine à la racine du repo est
   un agrégateur pur (`packaging: pom` + `<modules>`), pas le parent Maven des
   services — chaque service garde `spring-boot-starter-parent` comme parent
   et reste indépendant en build/déploiement. Son seul rôle est de permettre à
   IntelliJ (et à un `mvn validate` lancé depuis la racine) de découvrir tous
   les services automatiquement. Après le scaffold, recharge les projets
   Maven dans l'IDE (icône de rafraîchissement dans l'onglet Maven, ou clic
   droit sur `pom.xml` racine → "Reload"). Sans `pom.xml` racine, il faudrait
   sinon importer chaque `pom.xml` de service à la main
   ("Add as Maven Project").

4. **Corriger le pluriel de la route REST si besoin.** Le script pluralise
   naïvement (ajoute un "s"), donc `inventory` devient `/api/inventorys` —
   à corriger en `/api/inventories`. Vérifie `<Domaine>Controller.java` et le
   `README.md` généré.

5. **Ajouter les dépendances Maven spécifiques au domaine** dans `pom.xml`
   (ex. `spring-boot-starter-security` pour `user-service` si l'auth JWT est
   prévue). Le template n'embarque que web + actuator + data-jpa + H2 (H2
   sert uniquement à ce que `_template` tourne sans dépendance externe). Dès
   que le service gère de vraies données, bascule sur le pattern
   Postgres + Liquibase établi avec `user-service` (driver `postgresql`,
   `liquibase-core`, changelogs YAML dans `src/main/resources/db/changelog`,
   `ddl-auto: validate`) — voir la section Conventions du
   [`CLAUDE.md`](../../../CLAUDE.md) racine pour le détail exact des
   dépendances et de la config `application.yml`. Il faut aussi ajouter le
   service Postgres partagé à `docker-compose.yml` si ce n'est pas déjà fait
   pour un autre service, et une entrée dans
   `infra/postgres/init/01-init-databases.sql` pour la base dédiée.

6. **Étoffer le domaine réel** : champs de l'entité, du DTO, règles métier,
   validations. Le renommage `Example` → `<Domaine>` donne une structure CRUD
   qui compile, pas l'implémentation finale — complète-la selon les besoins
   fonctionnels du service (voir la roadmap du projet pour le palier concerné).

7. **Respecter les conventions de test déjà établies** (voir la section
   Conventions du [`CLAUDE.md`](../../../CLAUDE.md) racine) : tests unitaires
   sur la couche `service` en `*Test.java` (Mockito, pas de contexte Spring),
   tests d'intégration sur la couche `controller` en `*IT.java`
   (`@SpringBootTest` + `MockMvc`). Dans `_template` c'est une base H2 réelle ;
   dans un service réel sur Postgres, utilise Testcontainers via un
   `TestcontainersConfig` partagé (`@TestConfiguration` +
   `@Bean @ServiceConnection PostgreSQLContainer`, importé avec
   `@Import(TestcontainersConfig.class)`), pas H2 — reproduis le pattern de
   `user-service`.

8. **Vérifier avant de committer** — ne jamais supposer que ça build :
   ```bash
   docker compose build <service-name>
   docker compose up -d <service-name>
   ```
   Rappel : Docker Compose ne rebuild pas automatiquement sur un changement de
   code, il faut relancer `--build` (ou `docker compose build`) après toute
   modification. Lance aussi les tests dans un conteneur Maven si Maven n'est
   pas installé localement :
   ```bash
   docker run --rm -v "$(pwd)/services/<service-name>:/app" -w //app maven:3.9-eclipse-temurin-21 mvn -B verify
   ```
   Piège connu : si le service utilise Testcontainers (voir étape 7), ce
   `docker run` imbriqué échoue même avec `-v /var/run/docker.sock:/var/run/docker.sock`
   monté — Docker Desktop restreint l'accès au socket Docker depuis
   l'intérieur d'un conteneur ("Enhanced Container Isolation"). Dans ce cas,
   fie-toi plutôt à la vérification `docker compose up` (déploiement réel
   contre le vrai Postgres) ; lancer `mvn verify` en local (hors conteneur,
   donc avec Maven installé sur la machine hôte) fonctionne normalement.

## Limites connues du script

- Ne gère pas les domaines dont le pluriel est irrégulier (étape 4 ci-dessus).
- Ne touche pas au gateway — le routing vers le nouveau service dans la
  gateway reste une étape manuelle séparée (prévue au Palier 1/3 de la
  roadmap).
- Ne bascule pas automatiquement sur Postgres + Liquibase + Testcontainers
  (étape 5) — le template reste sur H2 pour rester simple à copier, ce
  basculement reste un geste manuel par service réel.
- Suppose que `services/_template` garde la forme actuelle (package
  `com.foodies.template`, classes `Template*`/`Example*`). Si la structure du
  template change significativement, le script devra être mis à jour en
  conséquence.
