-- Crée une base par service sur l'instance Postgres partagée.
-- N'est exécuté par Postgres qu'au tout premier démarrage du conteneur
-- (volume de données vide). Pour ajouter une base à un Postgres déjà
-- initialisé, exécute la ligne manuellement, ex. :
--   docker compose exec postgres psql -U foodies -c "CREATE DATABASE recipe_service_db"

SELECT 'CREATE DATABASE user_service_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_service_db')\gexec

SELECT 'CREATE DATABASE recipe_service_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'recipe_service_db')\gexec
