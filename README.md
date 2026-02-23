# 🌐 CityMate API Gateway

API Gateway centralisée pour le projet CityMate - Routing intelligent, validation JWT et gestion CORS.

**Rôle** : Point d'entrée unique pour toutes les APIs backend (USER, CITY, COMMUNITY)

---

## 🚀 Technologies

- **Spring Boot 3.3.7**
- **Spring Cloud Gateway**
- **JWT Validation** (JJWT 0.12.3)
- **Java 17**
- **Gradle 8.5**
- **Docker**

---

## 🏗️ Architecture
```
Application Mobile (Kotlin)
        ↓
   API GATEWAY (8090)
   - Routing
   - JWT Validation
   - CORS
   - Rate Limiting
        ↓
   ┌────────┬──────────┬────────────┐
   ↓        ↓          ↓            ↓
USER API  CITY API  COMMUNITY API
  8081      8082       8083
```

---

## 📋 Fonctionnalités

✅ **Routing intelligent**
- Route vers USER API (`/api/v1/auth/**`, `/api/v1/users/**`)
- Route vers CITY API (`/api/v1/pois/**`, `/api/v1/events/**`)
- Route vers COMMUNITY API (`/api/v1/forum/**`, `/api/v1/notifications/**`)

✅ **Validation JWT centralisée**
- Valide le token AVANT de router
- Bloque requêtes sans token (401)
- Endpoints publics : `/auth/register`, `/auth/login`

✅ **CORS configuré**
- Support multi-origin
- Credentials autorisés
- Headers personnalisés

✅ **Logs détaillés**
- Chaque requête loggée
- Validation JWT tracée

---

## 🔧 Installation

### Prérequis
- Java 17
- Gradle 8.5+
- Docker (optionnel)

### Avec Docker (Recommandé)
```bash
# 1. Cloner le projet
git clone https://github.com/VOTRE-USERNAME/citymate-api-gateway.git
cd citymate-api-gateway

# 2. Builder
./gradlew build -x test

# 3. Lancer avec Docker
docker build -t citymate-api-gateway .
docker run -p 8090:8090 \
  -e JWT_SECRET=votre_secret \
  citymate-api-gateway
```

### Sans Docker
```bash
# 1. Cloner
git clone https://github.com/VOTRE-USERNAME/citymate-api-gateway.git
cd citymate-api-gateway

# 2. Lancer
./gradlew bootRun
```

---

## 🔐 Configuration JWT

**Le Gateway partage le même JWT_SECRET que les APIs backend.**
```yaml
jwt:
  secret: ${JWT_SECRET:VotreCleSecreteTresFortePourLaProduction}
```

**En production** : Définir `JWT_SECRET` comme variable d'environnement.

---

## 🧪 Tests

### Test 1 : Health Check
```bash
curl http://localhost:8090/actuator/health
```

### Test 2 : Register (endpoint public)
```bash
curl -X POST http://localhost:8090/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"password123","firstName":"Test","lastName":"User","profileType":"STUDENT"}'
```

### Test 3 : Get User (avec JWT)
```bash
curl http://localhost:8090/api/v1/users/me \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📊 Endpoints

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/actuator/health` | Health check | Public |
| POST | `/api/v1/auth/register` | Créer compte | Public |
| POST | `/api/v1/auth/login` | Se connecter | Public |
| GET | `/api/v1/users/me` | Mon profil | JWT requis |
| GET | `/api/v1/users/{username}` | Profil public | JWT requis |

---

## 🐳 Docker

### Build
```bash
docker build -t citymate-api-gateway .
```

### Run
```bash
docker run -p 8090:8090 \
  -e JWT_SECRET=your_secret_key \
  -e SPRING_PROFILES_ACTIVE=docker \
  citymate-api-gateway
```

### Avec Docker Compose

Voir le projet `citymate-infrastructure`

---


