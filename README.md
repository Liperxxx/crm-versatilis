# CRM Versatilis

Sistema de CRM (Customer Relationship Management) desenvolvido para gerenciar clientes, leads, oportunidades, orçamentos, tarefas e relatórios.

## 🚀 Tecnologias Utilizadas

### Frontend
- HTML5, CSS3, JavaScript (Vanilla)
- Layout responsivo com CSS modular

### Backend
- Java 21 com Spring Boot
- Spring Security + JWT (autenticação)
- Spring Data JPA
- Maven

## 📁 Estrutura do Projeto

```
crm-versatilis/
├── Frontend/
│   ├── assets/          # Imagens e recursos estáticos
│   ├── css/             # Estilos modularizados
│   │   ├── components.css
│   │   ├── dashboard.css
│   │   ├── layout.css
│   │   ├── reset.css
│   │   └── variables.css
│   ├── js/              # Lógica do frontend
│   │   ├── app.js
│   │   ├── clientes.js
│   │   ├── dashboard.js
│   │   ├── leads.js
│   │   ├── oportunidades.js
│   │   ├── orcamentos.js
│   │   ├── produtos.js
│   │   ├── relatorios.js
│   │   └── tarefas.js
│   ├── index.html
│   ├── login.html
│   └── recuperar-senha.html
└── Backend/
    ├── src/
    │   └── main/
    │       ├── java/com/versatilis/crm/
    │       │   ├── config/         # Configurações (CORS, segurança)
    │       │   ├── controllers/    # Endpoints REST
    │       │   ├── dto/            # Data Transfer Objects
    │       │   ├── exceptions/     # Tratamento de erros
    │       │   ├── model/          # Entidades JPA
    │       │   ├── repositories/   # Repositórios JPA
    │       │   ├── security/       # JWT e autenticação
    │       │   └── services/       # Regras de negócio
    │       └── resources/
    │           ├── application.properties
    │           ├── application-dev.properties
    │           ├── application-prod.properties
    │           └── schema.sql
    ├── Dockerfile
    ├── docker-compose.yml
    └── pom.xml
```

## ⚙️ Funcionalidades

- 🔐 **Autenticação** — Login seguro com JWT
- 👥 **Clientes** — Cadastro e gestão de clientes
- 📋 **Leads** — Captação e acompanhamento de leads
- 💼 **Oportunidades** — Pipeline de vendas
- 📄 **Orçamentos** — Geração e envio de orçamentos em PDF
- ✅ **Tarefas** — Controle de atividades
- 📦 **Produtos** — Catálogo de produtos/serviços
- 📊 **Relatórios** — Análises e dashboards
- 📧 **E-mail** — Envio de e-mails integrado

## 🛠️ Como Executar

### Pré-requisitos
- Java 21+
- Maven
- Banco de dados configurado

### Backend

```bash
cd Backend
cp .env.example .env
# Configure as variáveis de ambiente no arquivo .env
./mvnw spring-boot:run
```

### Frontend

Abra o arquivo `Frontend/index.html` diretamente no navegador ou utilize um servidor local como o Live Server do VS Code.

### Com Docker

```bash
cd Backend
docker-compose up --build
```

## 🌿 Branches

| Branch | Descrição |
|--------|-----------|
| `main` | Código estável de produção |
| `develop` | Branch de desenvolvimento |
| `feature/*` | Novas funcionalidades |

## 📄 Variáveis de Ambiente

Copie o arquivo `.env.example` para `.env` e configure:

```env
# Banco de Dados
DB_URL=jdbc:postgresql://localhost:5432/crm_versatilis
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha

# JWT
JWT_SECRET=seu_secret_jwt
JWT_EXPIRATION=86400000

# E-mail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu_email@gmail.com
MAIL_PASSWORD=sua_senha
```

## 👤 Autor

**Liperxxx**  
GitHub: [@Liperxxx](https://github.com/Liperxxx)

---

> Projeto privado — Todos os direitos reservados © 2026