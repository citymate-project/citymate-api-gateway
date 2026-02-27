#  ARCHITECTURE CITYMATE - Documentation Complète

**Projet** : CityMate - Application mobile d'intégration urbaine  
**Équipe** : Master 2 TIIL-A - UBO  
**Tech Lead** : BRAHIM  
**Date** : février 2026  
**Présentation** : 14 janvier 2026

---

## 📊 VUE D'ENSEMBLE

### Architecture Micro-services

```
┌─────────────────────────────────────┐
│   Application Mobile (Kotlin)       │
│   Point d'entrée unique             │
│   http://localhost:8090             │
└───────────────┬─────────────────────┘
                │
                ↓
    ┌───────────────────────────┐
    │     API GATEWAY           │
    │       Port 8090           │
    │                           │
    │  ✅ Routing               │
    │  ✅ JWT Validation        │
    │  ✅ CORS                  │
    │  ✅ Rate Limiting         │
    │  ✅ Load Balancing        │
    └───────┬───────┬───────┬───┘
            │       │       │
    ┌───────┘       │       └────────┐
    ↓               ↓                ↓
┌────────┐     ┌────────┐     ┌──────────┐
│ USER   │     │ CITY   │     │COMMUNITY │
│ API    │     │ API    │     │   API    │
│ 8081   │     │ 8082   │     │   8083   │
│        │     │        │     │          │
│ Auth   │     │ POIs   │     │ Forum    │
│ Users  │     │ Events │     │ Notif    │
└───┬────┘     └───┬────┘     └────┬─────┘
    │              │                │
    ↓              ↓                ↓
┌────────┐     ┌────────┐     ┌──────────┐
│user_db │     │city_db │     │community │
│ 5432   │     │ 5433   │     │  _db     │
│        │     │ PostGIS│     │  5434    │
└────────┘     └────────┘     └──────────┘
         
              ┌────────┐
              │ Redis  │
              │ 6379   │
              │ Rate   │
              │Limiting│
              └────────┘
```

---

## 🎯 COMPOSANTS PRINCIPAUX

### 1. API GATEWAY (Port 8090)

**Rôle** : Point d'entrée unique pour toutes les requêtes

**Technologies** :
- Spring Boot 3.3.7
- Spring Cloud Gateway
- JJWT 0.12.3
- Redis (Rate Limiting)

**Responsabilités** :
- ✅ **Routing intelligent** : Dirige les requêtes vers la bonne API
- ✅ **Validation JWT** : Vérifie les tokens AVANT de router
- ✅ **Rate Limiting** : Limite à 100 req/sec par IP
- ✅ **CORS** : Autorise les requêtes cross-origin
- ✅ **Monitoring** : Expose `/actuator/health`

**Endpoints exposés** :
- `POST /api/v1/auth/register` → USER API
- `POST /api/v1/auth/login` → USER API
- `GET /api/v1/users/*` → USER API
- `GET /api/v1/pois/*` → CITY API (à venir)
- `GET /api/v1/forum/*` → COMMUNITY API (à venir)

---

### 2. USER API (Port 8081)

**Rôle** : Gestion authentification et utilisateurs

**Technologies** :
- Spring Boot 3.3.7
- Jersey (JAX-RS)
- Spring Security
- PostgreSQL
- JWT

**Responsabilités** :
- ✅ Authentification (register, login)
- ✅ Gestion utilisateurs
- ✅ Génération JWT
- ✅ Validation des credentials
- ✅ Gestion des rôles (VISITOR, CLIENT, ADMIN)

**Base de données** : `user_db` (PostgreSQL port 5432)

**Tables** :
- `users` : Informations utilisateurs
- `roles` : Rôles disponibles
- `user_roles` : Association users ↔ roles

---

### 3. CITY API (Port 8082) - À DÉVELOPPER

**Rôle** : Gestion des Points d'Intérêt, événements, bons plans

**Responsabilités** :
- POIs (restaurants, monuments, services)
- Événements locaux
- Bons plans et promotions
- Géolocalisation

**Base de données** : `city_db` (PostGIS port 5433)

---

### 4. COMMUNITY API (Port 8083) - À DÉVELOPPER

**Rôle** : Forum communautaire et notifications

**Responsabilités** :
- Forum de discussion
- Système de notifications
- Messages entre utilisateurs
- Fil d'actualité

**Base de données** : `community_db` (PostgreSQL port 5434)

---

### 5. Redis (Port 6379)

**Rôle** : Cache pour Rate Limiting

**Utilisation** :
- Compteur de requêtes par IP
- Token Bucket Algorithm
- Expiration automatique

---

## 🔐 SÉCURITÉ

### Flow d'authentification JWT

```
1. Utilisateur → POST /auth/register
   ↓
2. USER API crée le compte
   ↓
3. USER API génère un JWT token
   ↓
4. Token renvoyé au client
   ↓
5. Client stocke le token
   ↓
6. Requêtes suivantes : Authorization: Bearer <token>
   ↓
7. Gateway valide le JWT
   ↓
8. Si valide → Route vers l'API backend
   Si invalide → 401 Unauthorized
```

### JWT Structure

```json
{
  "sub": "username",
  "iat": 1234567890,
  "exp": 1234654290,
  "roles": ["CLIENT"]
}
```

**Secret partagé** : Tous les services utilisent le **même JWT_SECRET**

---

## 🔄 ROUTING

### Règles de routage

| Pattern | Destination | Authentification |
|---------|-------------|------------------|
| `/api/v1/auth/register` | USER API | Public |
| `/api/v1/auth/login` | USER API | Public |
| `/api/v1/users/**` | USER API | JWT requis |
| `/api/v1/pois/**` | CITY API | JWT requis |
| `/api/v1/events/**` | CITY API | JWT requis |
| `/api/v1/forum/**` | COMMUNITY API | JWT requis |
| `/actuator/health` | Gateway | Public |

---

## 🚦 RATE LIMITING

### Configuration actuelle

```yaml
replenishRate: 100       # 100 tokens par seconde
burstCapacity: 200       # Capacité maximale du bucket
requestedTokens: 1       # 1 token par requête
```

### Comportement

```
Utilisateur normal : 100 requêtes/seconde max
Pics de trafic : Burst jusqu'à 200 requêtes
Au-delà : HTTP 429 Too Many Requests
```

### Identification

**Par IP** : Chaque adresse IP a son propre compteur

---

## 🐳 DOCKER & DÉPLOIEMENT

### Structure Docker Compose

```yaml
services:
  # Bases de données
  - user-db (PostgreSQL)
  - city-db (PostGIS)
  - community-db (PostgreSQL)
  - redis (Cache)
  
  # APIs
  - user-api
  - api-gateway
```

### Commandes essentielles

```bash
# Tout démarrer
docker-compose up -d

# Voir les logs
docker-compose logs -f [service]

# Redémarrer un service
docker-compose restart [service]

# Rebuilder une API
docker-compose up -d --build [service]

# Tout arrêter
docker-compose down

# Tout supprimer (données incluses)
docker-compose down -v
```

---

## 🌐 URLS & PORTS

### En développement (local)

| Service | URL | Port |
|---------|-----|------|
| **API Gateway** | http://localhost:8090 | 8090 |
| USER API | http://localhost:8081 | 8081 |
| CITY API | http://localhost:8082 | 8082 |
| COMMUNITY API | http://localhost:8083 | 8083 |
| PostgreSQL User | localhost | 5432 |
| PostgreSQL City | localhost | 5433 |
| PostgreSQL Community | localhost | 5434 |
| Redis | localhost | 6379 |

### En Docker

**Les services communiquent via noms DNS** :
- `http://user-api:8081`
- `http://city-api:8082`
- `http://redis:6379`

---

## 🧪 TESTS

### Test manuel avec Postman

**1. Register** :
```
POST http://localhost:8090/api/v1/auth/register
Body: {
  "username": "alice",
  "email": "alice@test.com",
  "password": "password123",
  "firstName": "Alice",
  "lastName": "Test",
  "profileType": "STUDENT"
}
```

**2. Login** :
```
POST http://localhost:8090/api/v1/auth/login
Body: {
  "username": "alice",
  "password": "password123"
}
→ Copier le token
```

**3. Get User** :
```
GET http://localhost:8090/api/v1/users/me
Header: Authorization: Bearer <token>
```

### Test Rate Limiting

**Collection Runner Postman** : 105 iterations sur `/actuator/health`

**Résultat attendu** :
- Requêtes 1-100 : 200 OK
- Requêtes 101+ : 429 Too Many Requests

---

## 👥 GUIDE POUR L'ÉQUIPE

### Pour le développeur CITY API

**Ton API doit** :
1. Écouter sur le port **8082**
2. Utiliser le **même JWT_SECRET** que USER API
3. Exposer des endpoints sous `/api/v1/pois/**` et `/api/v1/events/**`
4. **NE PAS valider le JWT** (c'est fait par le Gateway)
5. Lire le header `X-Auth-Username` pour connaître l'utilisateur

**Ajout dans `docker-compose.yml`** :
```yaml
city-api:
  build: ../citymate-city-api
  ports:
    - "8082:8082"
  environment:
    JWT_SECRET: ${JWT_SECRET}
  depends_on:
    - city-db
```

**Ajout dans Gateway `application-docker.yml`** : Déjà configuré ! ✅

---

### Pour le développeur COMMUNITY API

**Même principe que CITY API** :
- Port **8083**
- Endpoints `/api/v1/forum/**`, `/api/v1/notifications/**`
- Pas de validation JWT (déjà fait par Gateway)
- Lire `X-Auth-Username` du header

---

### Pour le développeur Mobile (Kotlin)

**URL unique à utiliser** : `http://localhost:8090` (ou IP du serveur)

**Flow d'authentification** :

```kotlin
// 1. Register
val registerResponse = apiService.register(
    RegisterRequest(
        username = "alice",
        email = "alice@test.com",
        password = "password123"
    )
)

// 2. Stocker le token
val token = registerResponse.token
SharedPreferences.saveToken(token)

// 3. Requêtes suivantes avec le token
val retrofit = Retrofit.Builder()
    .baseUrl("http://localhost:8090")
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }
    .build()
```

**Gestion des erreurs** :

```kotlin
when (response.code()) {
    200 -> // Success
    401 -> // Token invalide → Redemander login
    429 -> // Rate limited → Ralentir les requêtes
    500 -> // Erreur serveur
}
```

---

## 🔧 CONFIGURATION

### Variables d'environnement

**À définir dans `.env`** (gitignored) :

```env
# JWT
JWT_SECRET=VotreCleSecreteTresFortePourLaProduction...

# Bases de données
USER_DB_USER=citymate_user
USER_DB_PASSWORD=user_password
CITY_DB_USER=citymate_city
CITY_DB_PASSWORD=city_password
COMMUNITY_DB_USER=citymate_community
COMMUNITY_DB_PASSWORD=community_password

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
```

---

## 📈 MONITORING

### Health Checks

**Gateway** :
```
GET http://localhost:8090/actuator/health
```

**USER API** :
```
GET http://localhost:8081/actuator/health
```

### Logs

```bash
# Tous les services
docker-compose logs -f

# Un service spécifique
docker-compose logs -f api-gateway
docker-compose logs -f user-api

# Dernières 100 lignes
docker-compose logs --tail=100 api-gateway
```

---

## 🐛 TROUBLESHOOTING

### Problème : Gateway ne démarre pas

**Solution** :
```bash
# Voir les logs d'erreur
docker-compose logs api-gateway

# Vérifier que Redis est lancé
docker-compose ps redis

# Rebuilder
docker-compose up -d --build api-gateway
```

---

### Problème : JWT invalide

**Cause** : JWT_SECRET différent entre Gateway et USER API

**Solution** : Vérifier que le secret est identique dans les deux services

---

### Problème : 429 Too Many Requests

**Cause** : Rate Limiting activé

**Solution** :
```bash
# Attendre quelques secondes
# OU augmenter les limites dans application-docker.yml
```

---

## 📚 RESSOURCES

### Documentation

- **Spring Cloud Gateway** : https://spring.io/projects/spring-cloud-gateway
- **JWT** : https://jwt.io
- **Redis** : https://redis.io

### Collections Postman

**Importer** : `docs/CityMate_API.postman_collection.json`

---

## 🎯 ROADMAP

### ✅ Phase 1 : Terminée
- API Gateway fonctionnel
- USER API complet
- JWT validation
- Rate Limiting
- Docker Compose

### 🔄 Phase 2 : En cours
- CITY API (POIs, Events)
- COMMUNITY API (Forum)
- Frontend Mobile (Kotlin)

### 📋 Phase 3 : À venir
- Circuit Breaker
- CI/CD avec GitHub Actions
- Tests automatisés
- Monitoring avancé

---

## 👥 CONTACTS ÉQUIPE

| Rôle | Nom | Responsabilité |
|------|-----|----------------|
| Tech Lead | BRAHIM | Architecture, USER API, Gateway |
| Dev Backend | [Nom] | CITY API |
| Dev Backend | [Nom] | COMMUNITY API |
| Dev Mobile | [Nom] | Application Kotlin |

---

## 📅 ÉCHÉANCES

**Présentation finale** : 14 Janvier 2026

**Objectifs** :
- ✅ Architecture micro-services fonctionnelle
- ✅ 3 APIs communicantes
- ✅ Application mobile connectée
- ✅ Déploiement Docker
- ✅ Tests validés

---

**Dernière mise à jour** : Février 2026  
**Version** : 1.0  
**Statut** : Production Ready ✅