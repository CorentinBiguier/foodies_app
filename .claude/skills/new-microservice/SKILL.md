---
name: new-microservice
description: Scaffolder un nouveau microservice Spring Boot dans le mono-repo Foodies à partir de services/_template (structure Maven en couches, Dockerfile, healthcheck, entrée docker-compose). Utiliser dès que l'utilisateur veut créer, ajouter ou démarrer un nouveau service (ex. user-service, recipe-service, inventory-service, scraper-service, ou tout autre nom en "xxx-service"), même s'il ne mentionne pas explicitement "scaffold" ou "template".
---

# Scaffolder un microservice

Ce skill duplique `services/_template` vers un nouveau service, en renommant le
package Java et en remplaçant le domaine de démo `Example` par le vrai domaine
du service (ex. `Example` → `User` pour `user-service`). Le résultat compile et
passe les tests immédiatement : c'est un point de départ fonctionnel à adapter,
pas un squelette vide.

La partie mécanique (copie, renommage de package/classes, entrée
docker-compose, port) est déléguée à `scripts/scaffold-service.sh` — ne la
refais pas à la main, le script est déterministe et évite les erreurs de
renommage partiel. Le reste de ce fichier couvre ce que le script ne peut pas
décider à ta place.

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
   libre, et génère un `README.md` de départ.

3. **Corriger le pluriel de la route REST si besoin.** Le script pluralise
   naïvement (ajoute un "s"), donc `inventory` devient `/api/inventorys` —
   à corriger en `/api/inventories`. Vérifie `<Domaine>Controller.java` et le
   `README.md` généré.

4. **Ajouter les dépendances Maven spécifiques au domaine** dans `pom.xml`
   (ex. `spring-boot-starter-security` pour `user-service` si l'auth JWT est
   prévue). Le template n'embarque que web + actuator + data-jpa + h2.

5. **Étoffer le domaine réel** : champs de l'entité, du DTO, règles métier,
   validations. Le renommage `Example` → `<Domaine>` donne une structure CRUD
   qui compile, pas l'implémentation finale — complète-la selon les besoins
   fonctionnels du service (voir la roadmap du projet pour le palier concerné).

6. **Respecter les conventions de test déjà établies** (voir la section
   Conventions du [`CLAUDE.md`](../../../CLAUDE.md) racine) : tests unitaires
   sur la couche `service` en `*Test.java` (Mockito, pas de contexte Spring),
   tests d'intégration sur la couche `controller` en `*IT.java`
   (`@SpringBootTest` + `MockMvc`, base H2 réelle).

7. **Vérifier avant de committer** — ne jamais supposer que ça build :
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

## Limites connues du script

- Ne gère pas les domaines dont le pluriel est irrégulier (étape 3 ci-dessus).
- Ne touche pas au gateway — le routing vers le nouveau service dans la
  gateway reste une étape manuelle séparée (prévue au Palier 1/3 de la
  roadmap).
- Suppose que `services/_template` garde la forme actuelle (package
  `com.foodies.template`, classes `Template*`/`Example*`). Si la structure du
  template change significativement, le script devra être mis à jour en
  conséquence.
