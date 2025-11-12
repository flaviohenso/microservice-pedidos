# Estágio 1: Build - Compila a aplicação
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Define o diretório de trabalho
WORKDIR /app

# Copia o arquivo pom.xml primeiro (para cache das dependências)
COPY pom.xml .

# Baixa as dependências (cache será usado se pom.xml não mudar)
RUN mvn dependency:go-offline -B

# Copia o código fonte
COPY src ./src

# Compila a aplicação e cria o JAR (sem executar testes)
RUN mvn clean package -DskipTests -B

# Estágio 2: Runtime - Imagem final otimizada
FROM eclipse-temurin:21-jre-alpine

# Cria um usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

# Define o diretório de trabalho
WORKDIR /app

# Copia o JAR do estágio de build
COPY --from=build /app/target/microservice-pedidos-*.jar app.jar

# Altera o dono do arquivo para o usuário spring
RUN chown spring:spring app.jar

# Muda para o usuário não-root
USER spring:spring

# Expõe a porta da aplicação
EXPOSE 8081

# Define variáveis de ambiente padrão
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando para executar a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

