package com.versatilis.crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfNotExists("usuarios", "avatar_url", "VARCHAR(500)");
        createConfigTableIfNotExists();
        createEnviosWhatsappTableIfNotExists();
        // Garante colunas herdadas de BaseEntity caso a tabela tenha sido criada antes do fix
        addColumnIfNotExists("envios_whatsapp", "data_criacao", "TIMESTAMP NOT NULL DEFAULT NOW()");
        addColumnIfNotExists("envios_whatsapp", "data_atualizacao", "TIMESTAMP");
        addColumnIfNotExists("envios_whatsapp", "ativo", "BOOLEAN NOT NULL DEFAULT TRUE");
        seedDefaultConfig("whatsapp.numero-empresa", "+55 27 99576-7070");

        // ── Marcenaria (calculadora de custos no módulo Orçamentos) ───────
        createMateriaisMarcenariaTable();
        createAcessoriosMarcenariaTable();
        createConfigMaoObraTable();
        // v2: ajudante + margem de lucro
        addColumnIfNotExists("config_mao_obra", "custo_diario_ajudante", "NUMERIC(12,2)");
        addColumnIfNotExists("config_mao_obra", "margem_lucro_padrao_pct", "NUMERIC(5,2) NOT NULL DEFAULT 30.00");
        // v2: persistência de cálculos
        createCalculosMarcenariaTables();
        seedMarcenaria();
    }

    private void seedDefaultConfig(String chave, String valorPadrao) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM configuracao_empresa WHERE chave = ?",
                Integer.class, chave);
            if (count == null || count == 0) {
                jdbcTemplate.update(
                    "INSERT INTO configuracao_empresa (chave, valor) VALUES (?, ?)",
                    chave, valorPadrao);
                log.info("Configuracao padrao semeada: {} = {}", chave, valorPadrao);
            }
        } catch (Exception e) {
            log.warn("Erro ao semear configuracao {}: {}", chave, e.getMessage());
        }
    }

    private void addColumnIfNotExists(String table, String column, String type) {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?)",
                Boolean.class, table, column
            );
            if (Boolean.FALSE.equals(exists)) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                log.info("Coluna {}.{} criada com sucesso", table, column);
            }
        } catch (Exception e) {
            log.warn("Erro ao verificar/criar coluna {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void createConfigTableIfNotExists() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS configuracao_empresa (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  chave VARCHAR(100) NOT NULL UNIQUE," +
                "  valor VARCHAR(1000)" +
                ")"
            );
            log.info("Tabela configuracao_empresa verificada/criada");
        } catch (Exception e) {
            log.warn("Erro ao criar tabela configuracao_empresa: {}", e.getMessage());
        }
    }

    private void createMateriaisMarcenariaTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS materiais_marcenaria (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  nome VARCHAR(255) NOT NULL," +
                "  categoria VARCHAR(30) NOT NULL," +
                "  espessura_mm INTEGER," +
                "  largura_chapa_mm INTEGER NOT NULL DEFAULT 2750," +
                "  altura_chapa_mm INTEGER NOT NULL DEFAULT 1850," +
                "  preco_chapa NUMERIC(12,2) NOT NULL," +
                "  fornecedor VARCHAR(255)," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            log.info("Tabela materiais_marcenaria verificada/criada");
        } catch (Exception e) {
            log.warn("Erro ao criar tabela materiais_marcenaria: {}", e.getMessage());
        }
    }

    private void createAcessoriosMarcenariaTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS acessorios_marcenaria (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  nome VARCHAR(255) NOT NULL," +
                "  categoria VARCHAR(30) NOT NULL," +
                "  unidade_medida VARCHAR(10) NOT NULL," +
                "  preco_unitario NUMERIC(12,2) NOT NULL," +
                "  fornecedor VARCHAR(255)," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            log.info("Tabela acessorios_marcenaria verificada/criada");
        } catch (Exception e) {
            log.warn("Erro ao criar tabela acessorios_marcenaria: {}", e.getMessage());
        }
    }

    private void createCalculosMarcenariaTables() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS calculos_marcenaria (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  nome VARCHAR(255) NOT NULL," +
                "  modo VARCHAR(20) NOT NULL," +
                "  dias_funcionario NUMERIC(6,2) NOT NULL," +
                "  dias_ajudante NUMERIC(6,2)," +
                "  custo_diario_funcionario_snapshot NUMERIC(12,2) NOT NULL," +
                "  custo_diario_ajudante_snapshot NUMERIC(12,2)," +
                "  margem_lucro_pct_snapshot NUMERIC(5,2) NOT NULL," +
                "  margem_corte_mm INTEGER," +
                "  permitir_rotacao BOOLEAN," +
                "  custo_materiais NUMERIC(12,2) NOT NULL," +
                "  custo_acessorios NUMERIC(12,2) NOT NULL," +
                "  custo_mao_obra NUMERIC(12,2) NOT NULL," +
                "  custo_producao NUMERIC(12,2) NOT NULL," +
                "  valor_lucro NUMERIC(12,2) NOT NULL," +
                "  preco_venda NUMERIC(12,2) NOT NULL," +
                "  observacoes TEXT," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS calculos_marcenaria_pecas (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  calculo_id BIGINT NOT NULL," +
                "  material_id BIGINT," +
                "  material_nome_snapshot VARCHAR(255)," +
                "  preco_chapa_snapshot NUMERIC(12,2)," +
                "  largura_mm INTEGER NOT NULL," +
                "  altura_mm INTEGER NOT NULL," +
                "  quantidade INTEGER NOT NULL," +
                "  descricao VARCHAR(255)," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS calculos_marcenaria_acessorios (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  calculo_id BIGINT NOT NULL," +
                "  acessorio_id BIGINT," +
                "  acessorio_nome_snapshot VARCHAR(255)," +
                "  preco_unitario_snapshot NUMERIC(12,2)," +
                "  quantidade NUMERIC(12,3) NOT NULL," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS calculos_marcenaria_areas (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  calculo_id BIGINT NOT NULL," +
                "  material_id BIGINT," +
                "  material_nome_snapshot VARCHAR(255)," +
                "  preco_chapa_snapshot NUMERIC(12,2)," +
                "  largura_chapa_snapshot INTEGER," +
                "  altura_chapa_snapshot INTEGER," +
                "  area_m2 NUMERIC(12,4) NOT NULL," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_calc_marc_pecas_calc ON calculos_marcenaria_pecas(calculo_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_calc_marc_acess_calc ON calculos_marcenaria_acessorios(calculo_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_calc_marc_areas_calc ON calculos_marcenaria_areas(calculo_id)");
            log.info("Tabelas calculos_marcenaria* verificadas/criadas");
        } catch (Exception e) {
            log.warn("Erro ao criar tabelas calculos_marcenaria*: {}", e.getMessage());
        }
    }

    private void createConfigMaoObraTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS config_mao_obra (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  custo_diario NUMERIC(12,2) NOT NULL DEFAULT 300.00," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            log.info("Tabela config_mao_obra verificada/criada");
        } catch (Exception e) {
            log.warn("Erro ao criar tabela config_mao_obra: {}", e.getMessage());
        }
    }

    private void seedMarcenaria() {
        try {
            Integer materiaisCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM materiais_marcenaria", Integer.class);
            if (materiaisCount != null && materiaisCount == 0) {
                Object[][] materiais = new Object[][]{
                    {"MDF Branco TX 15mm", "MDF",        15, 2750, 1850, "270.00"},
                    {"MDF Branco TX 18mm", "MDF",        18, 2750, 1850, "320.00"},
                    {"MDF Cru 15mm",       "MDF",        15, 2750, 1850, "220.00"},
                    {"MDP Branco 15mm",    "MDP",        15, 2750, 1850, "240.00"},
                    {"Compensado 15mm",    "COMPENSADO", 15, 2200, 1600, "180.00"},
                };
                for (Object[] m : materiais) {
                    jdbcTemplate.update(
                        "INSERT INTO materiais_marcenaria (nome, categoria, espessura_mm, largura_chapa_mm, altura_chapa_mm, preco_chapa, ativo, data_criacao) " +
                        "VALUES (?, ?, ?, ?, ?, CAST(? AS NUMERIC), TRUE, CURRENT_TIMESTAMP)",
                        m[0], m[1], m[2], m[3], m[4], m[5]);
                }
                log.info("Seed de materiais de marcenaria inseridos: {} itens", materiais.length);
            }

            Integer acessoriosCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM acessorios_marcenaria", Integer.class);
            if (acessoriosCount != null && acessoriosCount == 0) {
                Object[][] acessorios = new Object[][]{
                    {"Dobradiça Caneco 35mm",         "DOBRADICA",  "UN",  "6.50"},
                    {"Corrediça Telescópica 350mm",   "CORREDICA",  "PAR", "28.00"},
                    {"Puxador Alça 96mm",             "PUXADOR",    "UN",  "12.00"},
                    {"Parafuso 3,5x16mm (cento)",     "PARAFUSO",   "UN",  "8.00"},
                    {"Fita de Borda 22mm",            "FITA_BORDA", "M",   "1.20"},
                };
                for (Object[] a : acessorios) {
                    jdbcTemplate.update(
                        "INSERT INTO acessorios_marcenaria (nome, categoria, unidade_medida, preco_unitario, ativo, data_criacao) " +
                        "VALUES (?, ?, ?, CAST(? AS NUMERIC), TRUE, CURRENT_TIMESTAMP)",
                        a[0], a[1], a[2], a[3]);
                }
                log.info("Seed de acessórios de marcenaria inseridos: {} itens", acessorios.length);
            }

            Integer configCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM config_mao_obra", Integer.class);
            if (configCount != null && configCount == 0) {
                jdbcTemplate.update(
                    "INSERT INTO config_mao_obra (custo_diario, margem_lucro_padrao_pct, ativo, data_criacao) " +
                    "VALUES (300.00, 30.00, TRUE, CURRENT_TIMESTAMP)");
                log.info("Seed de config_mao_obra inserido: custo_diario R$ 300,00/dia, margem 30%");
            } else {
                // v2 backfill: garante margem padrão em registros pré-existentes (v1)
                int updated = jdbcTemplate.update(
                    "UPDATE config_mao_obra SET margem_lucro_padrao_pct = 30.00 WHERE margem_lucro_padrao_pct IS NULL");
                if (updated > 0) {
                    log.info("Backfill v2: margem padrão de {} registro(s) de config_mao_obra ajustada para 30%", updated);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao semear dados de marcenaria: {}", e.getMessage());
        }
    }

    private void createEnviosWhatsappTableIfNotExists() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS envios_whatsapp (" +
                "  id BIGSERIAL PRIMARY KEY," +
                "  orcamento_id BIGINT," +
                "  cliente_id BIGINT," +
                "  telefone VARCHAR(30) NOT NULL," +
                "  mensagem TEXT," +
                "  nome_arquivo VARCHAR(255)," +
                "  message_id VARCHAR(255)," +
                "  status VARCHAR(30) NOT NULL," +
                "  erro TEXT," +
                "  enviado_por VARCHAR(255)," +
                "  data_envio TIMESTAMP NOT NULL," +
                "  data_criacao TIMESTAMP NOT NULL DEFAULT NOW()," +
                "  data_atualizacao TIMESTAMP," +
                "  ativo BOOLEAN NOT NULL DEFAULT TRUE" +
                ")"
            );
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_envios_whatsapp_orcamento ON envios_whatsapp(orcamento_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_envios_whatsapp_message ON envios_whatsapp(message_id)");
            log.info("Tabela envios_whatsapp verificada/criada");
        } catch (Exception e) {
            log.warn("Erro ao criar tabela envios_whatsapp: {}", e.getMessage());
        }
    }
}
