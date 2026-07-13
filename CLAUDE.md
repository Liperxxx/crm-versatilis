# CLAUDE.md — CRM Versatilis

> Documento de orientação para o Claude. Comece SEMPRE por aqui (~150 linhas).
> Não leia o projeto inteiro: use o mapa abaixo para ir direto ao arquivo certo.

## 1. Domínio e propósito

CRM da arquiteta Versatilis. Pipeline comercial completo: leads → oportunidades →
clientes → orçamentos. Inclui módulos de tarefas, produtos, dashboard, relatórios
e — adicionado em abril/2026 — uma **calculadora de custos de marcenaria**
embutida no módulo Orçamentos (com packer 2D para corte de chapas, persistência
de cálculos e cálculo de margem de lucro / preço de venda).

Stack:
- **Backend:** Java 21 + Spring Boot + Spring Security (JWT) + Spring Data JPA + Lombok + Maven (`pom.xml` declara `<java.version>25</java.version>` para CI/Railway; localmente compila com 21 via `-Djava.version=21 -Dmaven.compiler.release=21`).
- **Frontend:** HTML5 + CSS modular + JavaScript vanilla (sem framework). 1 SPA (`Frontend/index.html`) com 1 arquivo JS singleton por módulo.
- **Banco:** PostgreSQL no Supabase (prod) — projeto `sqtttloncpfqysssrvyr` / host `db.sqtttloncpfqysssrvyr.supabase.co`. H2 in-memory em dev (`spring.profiles.active=dev`).
- **Infra:** Backend no Railway (deploy automático via push para `main`), frontend no Vercel.
- **Integrações:** Evolution API (WhatsApp), Resend (email), Supabase Storage (uploads).

## 2. Mapa do código (ir direto ao arquivo)

### Backend — pacote `com.versatilis.crm` (layout flat-by-layer)
- `controllers/` — endpoints REST. Sem `/api` no `@RequestMapping` (prefixo global em `application.properties` via `server.servlet.context-path=/api`). Sempre `ResponseEntity<ResponseDTO<T>>`.
- `services/` — regra de negócio. `@Transactional` em writes, `@Transactional(readOnly=true)` em reads. Mapeamento Entity↔DTO **manual** (`toDTO(entity)`); _ProdutoService_ usa ModelMapper como exceção legada.
- `model/` — entities JPA, todas estendem `BaseEntity` (`id`, `dataCriacao`, `dataAtualizacao`, `ativo`).
- `repositories/` — JpaRepository. Filtros opcionais via JPQL ternário `:campo IS NULL OR e.campo = :campo`.
- `dto/` — classes Lombok (`@Data @Builder`), validações Jakarta. `ResponseDTO<T>` é o envelope padrão.
- `security/` — `JwtAuthenticationFilter`, `JwtTokenProvider`, `SecurityConfig`. Papéis: `ADMIN`, `GERENTE`, `OPERADOR`.
- `exceptions/` — `ResourceNotFoundException`, `BadRequestException`. Tratamento global em `GlobalExceptionHandler` (`@RestControllerAdvice`).
- `config/SchemaMigrationRunner.java` — **importante**: roda no startup, cria tabelas e adiciona colunas idempotentemente (`CREATE TABLE IF NOT EXISTS` + `ADD COLUMN IF NOT EXISTS`). Também faz seed inicial de catálogos. Use ele para qualquer migration nova.
- `resources/{application,application-dev,application-prod}.properties` — perfis. `dev` usa H2 com `ddl-auto=update`; `prod` usa `ddl-auto=none` (todas as tabelas vêm do `SchemaMigrationRunner`).
- `resources/schema.sql` — só para ALTERs incrementais simples (não é a fonte primária de schema).

**Referência canônica de convenções:** [Backend/src/main/java/com/versatilis/crm/controllers/OrcamentoController.java](Backend/src/main/java/com/versatilis/crm/controllers/OrcamentoController.java) + [services/OrcamentoService.java](Backend/src/main/java/com/versatilis/crm/services/OrcamentoService.java). Seguir esse padrão para qualquer entidade nova.

### Frontend — `Frontend/`
- `index.html` — todos os módulos como `<section id="X-module" class="module">` dentro de `<main class="content">`. Sidebar com `<a data-module="X">`. Renderização dinâmica.
- `js/app.js` — bootstrap, define `API_BASE_URL` (localhost:8081 em dev / Railway em prod) e `window.CRMAuth` (`getToken`, `authHeaders`, `redirectToLogin`).
- `js/navigation.js` — gerencia troca de módulos via hash. Tem lista hardcoded de `validModules`.
- `js/notifications.js` — toasts globais (`toast(type, icon, msg, duration)`).
- `js/{clientes,leads,oportunidades,orcamentos,produtos,tarefas,dashboard,relatorios,perfil,configuracoes,marcenaria}.js` — 1 classe singleton por módulo (`init() / bindEvents() / render() / apiCreate() / apiUpdate() / apiDelete() / apiGetById()`). Padrão consistente — copiar de `orcamentos.js` ao criar módulo novo.
- `css/{variables,reset,layout,components,dashboard,marcenaria}.css` — variáveis em `variables.css` (paleta laranja `--color-primary: #CD5A26`, sidebar azul `--sidebar-bg: #112251`, espaçamento `--spacing-{sm,md,lg,xl}`, z-index `--z-modal`, etc.).

## 3. Regras de busca (não leia o que não precisa)

| Pergunta típica | Onde olhar primeiro |
|---|---|
| Como criar endpoint REST? | [OrcamentoController.java](Backend/src/main/java/com/versatilis/crm/controllers/OrcamentoController.java) |
| Como persistir entidade nova? | [OrcamentoService.java](Backend/src/main/java/com/versatilis/crm/services/OrcamentoService.java) (mapping manual) ou [ProdutoService.java](Backend/src/main/java/com/versatilis/crm/services/ProdutoService.java) (ModelMapper, legado) |
| Como adicionar coluna/tabela em prod? | [SchemaMigrationRunner.java](Backend/src/main/java/com/versatilis/crm/config/SchemaMigrationRunner.java) — incrementar `run()` com `addColumnIfNotExists` ou `CREATE TABLE IF NOT EXISTS` |
| Como autorizar rota nova? | [SecurityConfig.java](Backend/src/main/java/com/versatilis/crm/security/SecurityConfig.java) (`requestMatchers` por método/path) + `@PreAuthorize` no método |
| Como criar módulo no frontend? | Copiar [js/orcamentos.js](Frontend/js/orcamentos.js) e [css/marcenaria.css](Frontend/css/marcenaria.css) (mais recente, paleta moderna) |
| Como gerar PDF de orçamento? | `PdfService.gerarPdf(OrcamentoDTO)` — usado pelo WhatsApp e pelo `GET /orcamentos/{id}/pdf` |
| Como enviar WhatsApp? | `WhatsAppService` + `EvolutionApiClient` |

## 4. Comandos úteis

```bash
# Backend dev (H2 em memória, porta 8081)
cd Backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Backend build (override Java 21 localmente)
cd Backend && ./mvnw clean package -DskipTests -Djava.version=21 -Dmaven.compiler.release=21

# Backend rodando a partir do JAR
java -jar Backend/target/crm-versatilis-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=8081

# Frontend (qualquer estático na 5500 — origem permitida no CORS)
cd Frontend && python -m http.server 5500
```

## 5. Convenções (inferidas dos manifests + código existente)

**Backend:**
- Lombok em tudo (`@Data`, `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`).
- `@RequiredArgsConstructor` em controllers/services (constructor injection).
- `@Slf4j` + `log.info()` no início de cada método de controller (com endpoint e parâmetros).
- `BigDecimal` para valores monetários (`precision=12, scale=2`).
- Enums com `@Enumerated(EnumType.STRING)`.
- Soft delete: `setAtivo(false)`. **Nunca** `deleteById`.
- Validações Jakarta no DTO: `@NotNull`, `@NotBlank`, `@DecimalMin`, `@Min`, `@Size`. Controller usa `@Valid @RequestBody`.
- Cuidado com Race conditions ao gerar números sequenciais — `synchronized` (ver `OrcamentoService.gerarNumero()`).

**Frontend:**
- `localStorage` keys: `crm_token` (preferida), fallback `token` / `jwtToken`.
- Sempre `this.esc(value)` ao interpolar em `innerHTML` (XSS).
- `apiFetch()` centraliza interceptação de 401/403 e redireciona para login.
- Modais: classe `hidden` para esconder, `role="dialog" aria-modal="true"`.
- Tabs ARIA: `role="tab"` + `aria-selected` + setas para navegar.
- Currency: `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.
- Datas DB: ISO yyyy-MM-dd; exibição: `DD/MM/AAAA`.

**Git:**
- Commits semânticos (`feat:`, `fix:`, `chore:`, `refactor:` com escopo entre parênteses, ex: `feat(orcamentos): ...`).
- Merge para `main` deploy automático no Railway/Vercel.

## 6. Fluxo de trabalho com Claude

- `DEV/LOGS/` — diário curto do que foi feito por sessão. Ver [DEV/LOGS/](DEV/LOGS/).
- `DEV/LOGS/bug-fixes/` — receitas de fix com causa raiz para casos que tendem a voltar.
- `DEV/BACKLOG/active/` — tarefas em aberto que valem persistir entre sessões.
- `DEV/BACKLOG/completed/` — itens concluídos (move para cá ao fechar).

## 7. Conhecimento legado

Ver [DEV/LOGS/unified/legacy-knowledge.md](DEV/LOGS/unified/legacy-knowledge.md) — índice resumido dos documentos pré-existentes do projeto (README + Code Review de 11/04/2026).

## 8. README original

[README.md](README.md) — visão geral pública do projeto, instruções de execução, autor.
