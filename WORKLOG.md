## 05/04/2026
- Estrutura básica da página(/dashboard, /leads, /conversoes, /clientes, /auth, /oportunidades, /produtos, /relatorios, /tarefas, /usuarios) com login, validação e logout funcionais.
- Criação do README.md.
- Atualização do API_BASE_URL.
### Refatoração e Otimização do Dockerfile do Backend
- Build Automatizado: O container agora é responsável por compilar o projeto Spring Boot diretamente via Maven (mvn package -DskipTests), dispensando o build manual local.
- Atualização de versão: Upgrade da runtime do Java 17 para o Java 21 LTS.
- Segurança: Remoção de credenciais e redução da superfície de ataque ao utilizar uma imagem de execução limpa no estágio final.
## 06/04/2026
- Aprimoramento da API, Implementação do Supabase e Estilização.
- Sistema para cadastro de colaboradores e upload de avatar e logo de empresa.
- Adiciona sistema de CPF e CNPJ.
## 07/04/2026
- Carregamento de clientes no dropdown de orçamentos.
## 10/04/2026
- Permitiu usar o ddl-auto-update e adicionar usuário admin para autorizar produção.
- Conserto na tela de login.
## 11/04/2026
- Foi adicionado o auth guard
- Melhoramento no jwt.
- Correções de bugs.
- Import BigDecimal e atualizar clientes sem ModelMapper.
- Melhoramento do Dashboard.
## 20/04/2026
- Conserto do auth.
- Adicionado margens em ABNT para o pdf do orçamento.
## 27/04/2026
- Integração Whatsapp via API Evolution.
- Melhoramento do PDF para ficar devidamento acentuado as palavras e adiciona colunas.
- Adiciona jackson-databind no pom.xml
## 30/04/2026
- Adiciona função de imprimir para baixar PDF do backend.
## 22/05/2026
- Valor por item, PDF alinhado ao preview e ajustes dos comerciais.
## 23/05/2026
- Adiciona toggle no formulário para escolher entre: itemizado e só total. 
- Nova coluna para orcamentos.valor.total_manual, PdfService: lista simples no modo manual; tabela com valores no modo itemizado, orçamentos legados abrem só com "só total".
## 09/06/2026
- Foi retirado o @transctional do enviarOrcamento.
## 16/06/2026
- Adicionado o anexar fotos do projeto no PDF com um limite de 15MB (antes era 5MB).
## 13/07/2026
- Adicionado CLAUDE.md e estrutura DEV/.
## 17/07/2026
- Adicionado o DEV_GUIDE.md.
## 18/07/2026
- Altera os status direto na listagem sem ter que abrir mais o editor.
- Status preciso ao invés de 500.
- Conserto do ícone da aba de relatórios.
- Tela de aparência nas configurações funcional.
- Foi retirado o localStorage e agora passa a ser salvo no backend os dados.
- Adiciona opção de poder ver os dados da empresa no orçamento.
- Padroniza o backend para Java 25.
- Valores de orçamentos (ganhos/perdidos) + logo da Versatilis timbrado no PDF.
- Correção no Maven.
## 23/07/2026
- Dados de pagamento da Versatilis em todo PDF de orçamento.
## 27/07/2026
- Adiciona o WORKLOG.md.
## 10/08/2026
- Cliente, Oportunidade e Orcamento ganham criadoPor (Usuario), coluna
  criado_por_id.
- ClienteService/OportunidadeService/OrcamentoService.criar setam
  criadoPor = usuário logado (reusando security/UsuarioAtual).
