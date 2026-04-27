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
