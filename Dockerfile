# API do Nick Finance.
# Etapa 1 compila com o JDK; etapa 2 roda só com o JRE (imagem menor).

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Dependências primeiro, para o Docker reaproveitar o cache quando só o código muda.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 nickfinance
COPY --from=build /app/build/libs/app.jar app.jar
USER nickfinance

# Pensado para servidores pequenos (512 MB no plano gratuito do Render).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
