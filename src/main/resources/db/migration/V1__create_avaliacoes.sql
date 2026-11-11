CREATE TABLE avaliacoes (
    id UUID PRIMARY KEY,
    email_avaliador VARCHAR(320) NOT NULL,
    email_avaliado VARCHAR(320) NOT NULL,
    comentarios_avaliacao VARCHAR(5000) NOT NULL,
    nota NUMERIC(3, 1) NOT NULL CHECK (nota >= 0 AND nota <= 10),
    data_avaliacao TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_avaliacoes_data_avaliacao
    ON avaliacoes (data_avaliacao DESC);
