# Use uma imagem base oficial do OpenJDK
FROM eclipse-temurin:17-jdk-slim

# Defina o diretório de trabalho dentro do contêiner
WORKDIR /app

# Copie o arquivo JAR da sua aplicação para o diretório de trabalho
# O arquivo JAR é gerado pelo Maven na fase de build (target/crm-versatilis.jar)
COPY target/crm-versatilis.jar app.jar

# Defina as variáveis de ambiente padrão.
# Estas podem ser sobrescritas ao executar o contêiner.
ENV DB_URL="jdbc:mysql://db:3306/crm_versatilis?useSSL=false&serverTimezone=UTC"
ENV DB_USERNAME="crm_user"
ENV DB_PASSWORD="SenhaForte123!@#"
ENV JWT_SECRET="sua_chave_secreta_muito_segura_aqui_com_minimo_256_bits_e_mais_de_32_caracteres_para_HS512"
ENV JWT_EXPIRATION="86400000"
ENV APP_ENVIRONMENT="production"
ENV APP_DEBUG="false"
ENV HIBERNATE_DDL_AUTO="validate"

# Exponha a porta em que a aplicação Spring Boot será executada
EXPOSE 8080

# Comando para executar a aplicação quando o contêiner for iniciado
# Ativa o perfil de produção do Spring
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]