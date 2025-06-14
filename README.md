# SupplyCrate – Backend
- FELLAH Mohammed Nassim  – mednassim.fellah@gmail.com

## Présentation du projet

**SupplyCrate**, une application orientée microservices permettant de gérer un catalogue de produits à destination des fournisseurs et distributeurs.  
Ce backend repose sur **Spring Boot**, en suivant les principes de la **conception pilotée par le domaine (DDD)** pour mieux structurer la logique métier au sein de chaque microservice.  
L’ensemble des services expose des **API RESTful sécurisées**, avec une **authentification JWT**, une **configuration centralisée via Spring Cloud Config**, et une **découverte de services** assurée par **Eureka**.  

La communication entre services est orchestrée par une **API Gateway** jouant également le rôle de **load balancer** et intégrant un **circuit breaker** pour garantir la résilience face aux pannes.  
Le microservice de recherche (`ms-search`) repose sur **Elasticsearch** pour fournir une recherche rapide et puissante, couplée à **Redis** pour la mise en cache des résultats et suggestions (Saisie automatique lors de la recherche).  

L’objectif était de construire une architecture **modulaire**, **scalable** et **résiliente**, illustrant la puissance des services web modernes dans un environnement **cloud-ready**.

### Pourquoi cette idée ?

Le choix d’un projet de gestion de produits nous a permis d’implémenter des fonctionnalités variées, tout en respectant les contraintes imposées :
- Construction de plusieurs microservices distincts
- Mise en place d’une authentification sécurisée
- Intégration d’un système de recherche délégué à un microservice Python

### Architecture retenue

Le backend est divisé en plusieurs microservices indépendants :

| Microservice    | Rôle principal                                     | Technologie principale        |
|-----------------|----------------------------------------------------|-------------------------------|
| `ms-auth`       | Authentification, inscription, rôles (JWT)         | Spring Boot + Spring Security + MySQL |
| `ms-products`   | CRUD des produits, marques et catégories           | Spring Boot + Oracle          |
| `ms-search`     | Recherche avancée                                  | Django (Python) + ElasticSearch + PostgreSQL + Redis (caching)          |
| `ms-gateway`    | Point d’entrée global, redirection et filtrage     | Spring Cloud Gateway          |
| `ms-registery`  | Service Discovery (enregistrement des services)    | Eureka                        |
| `config-server` | Configuration centralisée                          | Spring Cloud Config Server    |
| `common-config` | Fichiers de configuration partagés                 | Spring Boot                   |

Chaque service est indépendant et peut être démarré, testé ou modifié sans impacter les autres.

## Technologies utilisées

### Frameworks & Langages

- **Java 17 / 21**
- **Spring Boot 3.2+** : utilisé pour l’ensemble des microservices
- **Spring Cloud 2023.0.x** : pour Eureka, Gateway, Config Server, OpenFeign
- **Spring Security + JWT** : pour protéger les routes avec des rôles `ADMIN` et `USER`
- **Django** : utilisé dans `ms-search` pour illustrer une architecture polyglotte

### Modules & Librairies

- **Spring Cloud Gateway** : API Gateway pour centraliser les appels aux microservices
- **Feign Client** : communication interservices via des interfaces Java
- **Spring Boot Actuator** : surveillance des métriques de santé et de performance
- **Webflux** : programmation réactive dans la gateway
- **Lombok** : simplifie le code Java en générant getters/setters/constructeurs
- **Hibernate Validator** / **Jakarta Validation** : validation des données
- **Kafka** : communication asynchrone pour certains modules (pub/sub)
- **Spring Mail** : envoi d’e-mails
- **Commons CSV** : gestion d’exports ou d’imports de fichiers CSV

### Bases de données

- **Oracle** : pour le stockage des produits, catégories et marques
- **MySQL** : pour les utilisateurs et l’authentification
- **PostgreSQL** : pour le projet django

### Sécurité

- **JWT (Bearer Token)** : authentification basée sur des tokens signés
- **OAuth2** : pour la connexion via Google

### Autres outils

- **Postman** : tests manuels des endpoints (authentifiés et non authentifiés)
- **Docker** : utilisé pour contenir et lancer les services facilement

## Installation

Cette section explique comment installer et lancer le projet backend en local.

### Prérequis

Avant de commencer, il faut avoir :

- **Java 17 ou 21** – pour exécuter les microservices Spring Boot
- **Maven** – pour compiler et lancer les projets
- **MySQL** – pour la base de données des utilisateurs (`ms-auth`)
- **Oracle Database** – pour la base de données des produits (`ms-products`)
- **Python 3** – pour exécuter le service `ms-search` (Django)
- **Postman** – pour tester les endpoints API
- Un éditeur comme **IntelliJ IDEA**, **VS Code**, ou tout autre IDE Java compatible

---

### Étapes d'installation

1. **Cloner le projet :**

```bash
git clone [https://github.com/ZuxxLo/supply-crate.git](https://github.com/ZuxxLo/supply-crate.git)
cd supply-crate-backend
```

2. **Configurer les bases de données :**

- Vérifiez que les identifiants de connexion dans les fichiers de configuration sont corrects.
- Assurez-vous que les ports utilisés sont bien disponibles sur votre machine.
- Les fichiers de configuration sont centralisés ici :  
  [https://gitlab.dpt-info.univ-littoral.fr/fellah.mohammednassim/cloud-supply-crate.git](https://gitlab.dpt-info.univ-littoral.fr/fellah.mohammednassim/cloud-supply-crate.git)

3. **Lancer les microservices (dans cet ordre) :**

```bash
mvn spring-boot:run
```

Ordre recommandé :
- `ms-registery` (Eureka)
- `config-server`
- `ms-auth`
- `ms-products`
- `ms-gateway`
- `ms-search` (à exécuter via Django)

4. **Tester les services :**

Nous avons utilisé **Postman** pour tester tous les endpoints (`GET`, `POST`, `PUT`, `DELETE`), y compris ceux protégés par des tokens JWT.

---

## Utilisation

Une fois tous les services lancés, vous pouvez interagir avec l’API via **Postman** ou en connectant notre interface frontend.
> Le projet fonctionne aussi bien sous **Windows** que sous **Linux** .
## Routes d'API

Toutes les routes passent par le **gateway** :  
`http://localhost:7777`

La collection utilisée pour les tests est : `supply-crate-backend\Supply Crate Api.postman_collection.json`.  
Cette collection contient toutes les **requêtes**, leurs **headers**, **corps (body)** si nécessaire, ainsi que les **résultats** de test.  
L’API est protégée par JWT. Un token doit être envoyé dans l’en-tête :  
`Authorization: Bearer <token>`


---

### Admin Routes

#### Categories

```
[POST]   http://localhost:7777/service-products/api/categories
         → Créer une nouvelle catégorie

[PUT]    http://localhost:7777/service-products/api/categories/{id}
         → Modifier une catégorie

[DELETE] http://localhost:7777/service-products/api/categories/{id}
         → Supprimer une catégorie
```

####  Brands

```
[POST]   http://localhost:7777/service-products/api/brands
         → Créer une nouvelle marque

[PUT]    http://localhost:7777/service-products/api/brands/{id}
         → Modifier une marque

[DELETE] http://localhost:7777/service-products/api/brands/{id}
         → Supprimer une marque

[POST]   http://localhost:7777/service-products/api/brands/import-csv
         → Importer des marques depuis un fichier CSV
```

####  Products

```
[POST]   http://localhost:7777/service-products/api/products/import-csv
         → Importer des produits depuis un fichier CSV
```


### ms-products – User access

#### Categories

```
[GET] http://localhost:7777/service-products/api/categories/{id}
      → Récupérer une catégorie spécifique

[GET] http://localhost:7777/service-products/api/categories
      → Récupérer toutes les catégories avec pagination
```

#### Products

```
[GET]  http://localhost:7777/service-products/api/products/{id}
       → Détails d’un produit

[GET]  http://localhost:7777/service-products/api/products/by-user
       → Produits créés par l’utilisateur connecté

[GET]  http://localhost:7777/service-products/api/products
       → Tous les produits (avec pagination)

[POST] http://localhost:7777/service-products/api/products
       → Créer un produit

[PUT]  http://localhost:7777/service-products/api/products/{id}
       → Modifier un produit

[DELETE] http://localhost:7777/service-products/api/products/{id}
         → Supprimer un produit
```

####  Brands

```
[GET] http://localhost:7777/service-products/api/brands/{id}
      → Récupérer une marque

[GET] http://localhost:7777/service-products/api/brands
      → Récupérer toutes les marques avec pagination
```

---

### ms-auth – Authentification

```
[POST] http://localhost:7777/service-auth/api/users/register
       → Créer un compte utilisateur

[POST] http://localhost:7777/service-auth/api/users/login
       → Connexion

[GET]  http://localhost:7777/service-auth/api/users/verify-email
       → Vérification de l’adresse email

[GET]  http://localhost:7777/service-auth/api/users/get-all
       → Récupérer tous les utilisateurs

[GET]  http://localhost:7777/service-auth/api/users/google
       → Connexion via Google
```

---

### ms-search – Recherche (Elasticsearch)

```
[GET] http://localhost:7777/service-search/search/?q=Zidane&category=Tshirts&brand=Nike&maxPrice=1000&sort=ascending
      → Recherche globale avec filtres, tri, et cache Redis

[GET] http://localhost:7777/service-search/suggest/?q=w
      → Suggestions de recherche (auto-complétion)

[GET] http://localhost:7777/service-search/api/search
→ Recherche globale des produits

[GET] http://localhost:7777/service-search/api/search/suggest
→ Suggestions de recherche (auto-complétion)

```
## Déploiement avec Docker

Une branche `deploy` contient tous les fichiers nécessaires au déploiement.  
Chaque microservice dispose de son propre `Dockerfile`. Le fichier `docker-compose.yml` est situé dans :
`docker-compose.yml` de la branche develop

---

### Étapes pour lancer tout le projet :

1. **Générer les fichiers `.jar` des microservices Spring Boot** :

```bash
Windows: mvnw.cmd clean package -DskipTests 
Linux:  ./mvnw clean package -DskipTests 
```

2. **Lancer tous les services avec Docker Compose** :

```bash
docker compose up --build
```

3. **Accès via le Gateway** :

Tous les endpoints sont accessibles depuis :  
`http://localhost:7777`

Il s'agit du **seul point d'entrée autorisé** pour l’ensemble des microservices.  
Toute requête envoyée directement à un service sans passer par la **gateway** est **bloquée par la sécurité** (Spring Security + Gateway filters).


> Assurez-vous que les ports (7777, 8761, 3306, 1521...) sont libres avant le lancement.

---
# Clarification des microservices

Voici un résumé du rôle de chaque microservice de l’architecture **SupplyCrate** :

- **`ms-auth`** : gère l’enregistrement, l’authentification, les rôles, la vérification d’e-mail et la connexion via Google.
- **`ms-products`** : gère les produits, les marques, les catégories, les opérations CRUD et l'import CSV.
- **`ms-search`** : microservice Python Django connecté à **Elasticsearch** et **Redis**, chargé de la recherche avancée et des suggestions(autocomplete).
- **`ms-gateway`** : point d’entrée unique de toute l’application, centralise les appels aux services.
- **`ms-registery`** : service Eureka pour l’enregistrement et la découverte des services.
- **`config-server`** : centralise la configuration partagée entre les microservices.
- **`common-config`** : contient les fichiers YAML de configuration partagés.
- **`oracle-init-scripts`** : initialise la base Oracle avec les bons schémas et structures.

---

## Intégration d’APIs externes

Dans le cadre du projet, et en respect des bonnes pratiques demandées (consommation des APIs **côté backend uniquement**), nous avons intégré plusieurs services web externes :

### Authentification via Google (OAuth2)
- Permet à l’utilisateur de se connecter avec son compte Google.
- Le backend interagit avec l’API OAuth2 de Google, puis génère un **token JWT** valide pour l’application.
- Endpoint :
  ```
  [GET] http://localhost:7777/service-auth/api/users/google
  ```

### Envoi automatique d’e-mails
- Utilisation du service **Gmail SMTP** pour envoyer des e-mails de confirmation, d’inscription ou de vérification.
- Intégré au backend Spring Boot à l’aide du module `Spring Mail`.

###  Conversion de prix (multi-devises)
- Le backend interroge une API externe de **taux de change** afin de convertir dynamiquement les prix des produits.
- Prépare l’application à une gestion **internationale** des produits et utilisateurs.
___

### Communication et intelligence du système

Lorsque l’utilisateur **ajoute, modifie ou supprime un produit** via `ms-products`, un **événement Kafka** est automatiquement publié.  
Ce message est consommé par `ms-search`, qui **synchronise les données dans Elasticsearch** en conséquence (indexation, mise à jour ou suppression).

Lorsqu’un utilisateur effectue une recherche :
- Si les résultats sont déjà présents en cache (**Redis**), ils sont retournés immédiatement.
- Sinon, une requête est faite à **Elasticsearch**, et le résultat est ensuite mis en cache.

La suggestion (`/suggest?q=...`) fonctionne sur la base des données indexées dans Elasticsearch pour proposer une auto-complétion rapide et pertinente.

Resumé: 
Kafka a été utilisé pour assurer la communication entre le microservice ms-products (Spring Boot) et ms-search (Django), afin de garantir l’indexation correcte des produits dans Elasticsearch lors de la création, modification ou suppression d’un produit.
Redis a été utilisé pour mettre en cache les résultats de recherche et améliorer les performances.

