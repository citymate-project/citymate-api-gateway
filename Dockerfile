# ============================================
# CITYMATE API GATEWAY - Dockerfile
# ============================================

# ============================================
# ÉTAPE 1 : BUILD
# ============================================
FROM gradle:8.5-jdk17-alpine AS build

WORKDIR /app

# Copier tout
COPY . .

# Build intelligent :
# - Si build/libs existe → skip compilation
# - Sinon → compile tout
RUN if [ -d "build/libs" ] && [ -n "$(ls -A build/libs/*.jar 2>/dev/null)" ]; then \
        echo "JAR exists, skipping build"; \
    else \
        echo "Building from source..."; \
        chmod +x ./gradlew && \
        ./gradlew build -x test --no-daemon; \
    fi

# ============================================
# ÉTAPE 2 : RUNTIME
# ============================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/build/libs/*.jar app.jar

# Exposer le port 8080
EXPOSE 8090

# Variables d'environnement par défaut
ENV SPRING_PROFILES_ACTIVE=docker

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]

