-- Colunas adicionadas ao modelo Cliente (executado na inicialização se ausentes)
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS segmento   VARCHAR(100);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS observacoes TEXT;
