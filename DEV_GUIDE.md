┌────────────────────────┐              ┌────────────────────────┐  
│     FRONTEND (JS)      │              │  BACKEND (Spring Boot) │  
└────────────────────────┘              └────────────────────────┘  
            │                                       │  
            │  1. POST /api/auth/login       │  
            ├──────────────────────────────────────>│ (Valida credenciais)  
            │                                       │ (Gera JWT)  
            │  2. Retorna Token JWT          │  
            │<──────────────────────────────────────┤  
     (Salva no LocalStorage)                        │  
            │                                       │  
            │  3. Requisição + Header Authorization │  
            │     Bearer <token>             │  
            ├──────────────────────────────────────>│ (Spring Security)  
            │                                       │ (Valida Token JWT)  
            │                                       │ (Processa Serviço)  
            │  4. Retorna JSON com os dados         │  
            │<──────────────────────────────────────┤  

Frontend: Escrito em JavaScript Vanilla puro. Cada tela possui seu script de ciclo de vida isolado na pasta js/ (ex: clientes.js, leads.js), controlando o DOM e as chamadas de API.  
Backend: O Spring Security atua como um filtro interceptor. Toda requisição privada exige o cabeçalho Authorization: Bearer <JWT>. Caso o token tenha expirado ou seja inválido, o backend bloqueia o acesso antes mesmo de tocar nos Controllers.  

Camadas internas do Backend
Para manter o código seguro e isolar processos.
[Requisição HTTP] ──> Controllers (Endpoints & Validações de DTO)  
                               │  
                               ▼  
                        Services (Regras de Negócio e Transações)  
                               │  
                               ▼  
                        Repositories (Spring Data JPA / Queries SQL)  
                               │  
                               ▼  
                        [Banco de Dados PostgreSQL]  

Gestão de Ambientes e Configurações  
O sistema utiliza propriedades isoladas para garantir que o ambiente de desenvolvimento local não interfira com a base de produção. É controlado pelo Spring Boot.  
resources/  
├── application.properties          - configurações globais e seleção de perfil  
├── application-dev.properties      - banco de testes local (geralmente H2 ou Docker)  
└── application-prod.properties     - banco PostgreSQL oficial e SMTP de produção  
As credenciais sensíveis nunca devem ser salvas diretamente nestes arquivos. Pois são injetadas através do arquivo .env referenciado no ambiente host ou no docker-compose.yml.  

Pipeline de Vendas e Módulos Estratégicos  
Módulo de Leads: Captação inicial e qualificação do cliente em potencial.
Módulo de Oportunidades: Pipeline de vendas que guia o lead pelas etapas comerciais.
Módulo de Orçamentos: Acoplado a uma biblioteca geradora de PDF que exporta as propostas comerciais e as dispara via e-mail integrado.
