# ============================================
# CITYMATE API GATEWAY - Dockerfile
# ============================================

# ============================================
# ÉTAPE 1 : BUILD
# ============================================
FROM gradle:8.5-jdk17-alpine AS build

WORKDIR /app

# Copier les fichiers de dépendances en premier (cache Docker)
COPY build.gradle settings.gradle ./
COPY gradle gradle

# Télécharger les dépendances (layer mis en cache si build.gradle ne change pas)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copier le reste du code source
COPY src src

# Toujours supprimer l'ancien build et recompiler depuis les sources
RUN ./gradlew clean build -x test --no-daemon

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

