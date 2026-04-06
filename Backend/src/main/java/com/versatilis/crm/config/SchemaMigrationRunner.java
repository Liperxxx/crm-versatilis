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
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS configuracao_empresa (
                    id BIGSERIAL PRIMARY KEY,
                    chave VARCHAR(100) NOT NULL UNIQUE,
                    valor VARCHAR(1000)
                )
            """);
            log.info("Tabela configuracao_empresa verificada/criada");
        } catch (Exception e) {
            log.warn("Erro ao criar tabela configuracao_empresa: {}", e.getMessage());
        }
    }
}
