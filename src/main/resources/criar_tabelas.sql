-- ============================================================
-- EUColab / ISysHolCol  -  Trabalho.2  -  Modelo fisico (PostgreSQL)
-- ============================================================
CREATE SCHEMA holcol;
SET search_path TO holcol;

-- ============================================================
-- MORADA: codigo_postal -> cidade, pais  (3NF)
-- A cidade e o pais dependem do codigo postal e nao do cliente,
-- por isso ficam numa relacao propria.
-- ============================================================
CREATE TABLE codigo_postal (
    codigo_postal VARCHAR(10)  NOT NULL,
    pais          VARCHAR(60)  NOT NULL,
    cidade        VARCHAR(80)  NOT NULL,
    CONSTRAINT pk_codigo_postal PRIMARY KEY (codigo_postal, pais)
);

-- ============================================================
-- CLIENTE (superclasse) e subclasses PESSOA_SINGULAR / PESSOA_COLETIVA
-- Especializacao total e disjunta: 'tipo' + chave estrangeira composta
-- ============================================================
CREATE TABLE cliente (
    nif           BIGINT       NOT NULL,
    tipo          CHAR(1)      NOT NULL,
    email         VARCHAR(120) NOT NULL,
    rua           VARCHAR(120) NOT NULL,
    num_porta     VARCHAR(10)  NOT NULL,
    andar         VARCHAR(10),
    letra         VARCHAR(5),
    codigo_postal VARCHAR(10)  NOT NULL,
    pais          VARCHAR(60)  NOT NULL,
    CONSTRAINT pk_cliente PRIMARY KEY (nif),
    CONSTRAINT uq_cliente_nif_tipo UNIQUE (nif, tipo),
    CONSTRAINT uq_cliente_email UNIQUE (email),
    CONSTRAINT ck_cliente_nif CHECK (nif BETWEEN 100000000 AND 999999999),
    CONSTRAINT ck_cliente_tipo CHECK (tipo IN ('S','C')),
    CONSTRAINT ck_cliente_email CHECK (email LIKE '%_@_%.__%'),
    CONSTRAINT fk_cliente_cp FOREIGN KEY (codigo_postal, pais)
        REFERENCES codigo_postal (codigo_postal, pais)
);

CREATE TABLE pessoa_singular (
    nif             BIGINT      NOT NULL,
    tipo            CHAR(1)     NOT NULL DEFAULT 'S',
    nome_proprio    VARCHAR(60) NOT NULL,
    apelido         VARCHAR(60) NOT NULL,
    telemovel       VARCHAR(20) NOT NULL,
    data_nascimento DATE        NOT NULL,
    CONSTRAINT pk_pessoa_singular PRIMARY KEY (nif),
    CONSTRAINT uq_pessoa_singular_nif_tipo UNIQUE (nif, tipo),
    CONSTRAINT ck_ps_tipo CHECK (tipo = 'S'),
    CONSTRAINT ck_ps_nascimento CHECK (data_nascimento > DATE '1900-01-01'),
    CONSTRAINT fk_ps_cliente FOREIGN KEY (nif, tipo)
        REFERENCES cliente (nif, tipo) ON DELETE CASCADE
);

CREATE TABLE pessoa_coletiva (
    nif         BIGINT       NOT NULL,
    tipo        CHAR(1)      NOT NULL DEFAULT 'C',
    designacao  VARCHAR(120) NOT NULL,
    CONSTRAINT pk_pessoa_coletiva PRIMARY KEY (nif),
    CONSTRAINT ck_pc_tipo CHECK (tipo = 'C'),
    CONSTRAINT fk_pc_cliente FOREIGN KEY (nif, tipo)
        REFERENCES cliente (nif, tipo) ON DELETE CASCADE
);

-- Atributo multivalor composto (ate 5 contactos): o numero de ordem 1..5
-- garante o maximo de 5 contactos por pessoa coletiva.
CREATE TABLE contacto (
    nif_coletiva BIGINT      NOT NULL,
    num_contacto SMALLINT    NOT NULL,
    nome         VARCHAR(100) NOT NULL,
    text_chat    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_contacto PRIMARY KEY (nif_coletiva, num_contacto),
    CONSTRAINT ck_contacto_num CHECK (num_contacto BETWEEN 1 AND 5),
    CONSTRAINT fk_contacto_pc FOREIGN KEY (nif_coletiva)
        REFERENCES pessoa_coletiva (nif) ON DELETE CASCADE
);

-- ============================================================
-- EMPREGADO: uma pessoa (singular) que tambem e cliente
-- ============================================================
CREATE TABLE empregado (
    num_empregado  INTEGER NOT NULL,
    nif            BIGINT  NOT NULL,
    data_admissao  DATE    NOT NULL,
    CONSTRAINT pk_empregado PRIMARY KEY (num_empregado),
    CONSTRAINT uq_empregado_nif UNIQUE (nif),
    CONSTRAINT fk_empregado_ps FOREIGN KEY (nif)
        REFERENCES pessoa_singular (nif)
);

-- ============================================================
-- METODO_PAGAMENTO e subclasses MBWAY / CARTAO_CREDITO
-- ============================================================
CREATE TABLE metodo_pagamento (
    id          INTEGER GENERATED ALWAYS AS IDENTITY,
    nif_cliente BIGINT  NOT NULL,
    tipo        VARCHAR(10) NOT NULL,
    por_omissao BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_metodo_pagamento PRIMARY KEY (id),
    CONSTRAINT uq_metodo_id_tipo UNIQUE (id, tipo),
    CONSTRAINT ck_metodo_tipo CHECK (tipo IN ('MBWAY','CARTAO')),
    CONSTRAINT fk_metodo_cliente FOREIGN KEY (nif_cliente)
        REFERENCES cliente (nif) ON DELETE CASCADE
);
-- no maximo um metodo por omissao por cliente
CREATE UNIQUE INDEX uq_metodo_por_omissao
    ON metodo_pagamento (nif_cliente) WHERE por_omissao;

CREATE TABLE mbway (
    id        INTEGER     NOT NULL,
    tipo      VARCHAR(10) NOT NULL DEFAULT 'MBWAY',
    telemovel VARCHAR(20) NOT NULL,
    CONSTRAINT pk_mbway PRIMARY KEY (id),
    CONSTRAINT ck_mbway_tipo CHECK (tipo = 'MBWAY'),
    CONSTRAINT fk_mbway_metodo FOREIGN KEY (id, tipo)
        REFERENCES metodo_pagamento (id, tipo) ON DELETE CASCADE
);

CREATE TABLE cartao_credito (
    id            INTEGER     NOT NULL,
    tipo          VARCHAR(10) NOT NULL DEFAULT 'CARTAO',
    nome_cartao   VARCHAR(80) NOT NULL,
    numero        VARCHAR(19) NOT NULL,
    data_validade DATE        NOT NULL,
    codigo        VARCHAR(4)  NOT NULL,
    CONSTRAINT pk_cartao PRIMARY KEY (id),
    CONSTRAINT ck_cartao_tipo CHECK (tipo = 'CARTAO'),
    CONSTRAINT ck_cartao_numero CHECK (numero ~ '^[0-9]{13,19}$'),
    CONSTRAINT ck_cartao_codigo CHECK (codigo ~ '^[0-9]{3,4}$'),
    CONSTRAINT fk_cartao_metodo FOREIGN KEY (id, tipo)
        REFERENCES metodo_pagamento (id, tipo) ON DELETE CASCADE
);

-- ============================================================
-- SERVICO e subclasses ROOM_COLAB / TEXT_CHAT / TEXT_BLOG
-- Especializacao total e disjunta. O criador esta no proprio servico
-- (associacoes ASSOCIADO_A e CRIA_BLOG com cardinalidade N:1, total).
-- ============================================================
CREATE TABLE servico (
    id                 INTEGER GENERATED ALWAYS AS IDENTITY,
    tipo               VARCHAR(10)   NOT NULL,
    designacao         VARCHAR(100)  NOT NULL,
    nif_criador        BIGINT        NOT NULL,
    data_criacao       DATE          NOT NULL DEFAULT CURRENT_DATE,
    data_eliminacao    DATE,
    modalidade         VARCHAR(10)   NOT NULL DEFAULT 'FIXO',
    valor_fixo_mensal  NUMERIC(8,2)  NOT NULL DEFAULT 0,
    valor_por_membro   NUMERIC(8,2)  NOT NULL DEFAULT 0,
    CONSTRAINT pk_servico PRIMARY KEY (id),
    CONSTRAINT uq_servico_id_tipo UNIQUE (id, tipo),
    CONSTRAINT ck_servico_tipo CHECK (tipo IN ('ROOM_COLAB','TEXT_CHAT','TEXT_BLOG')),
    CONSTRAINT ck_servico_modalidade CHECK (modalidade IN ('FIXO','VARIAVEL')),
    CONSTRAINT ck_servico_eliminacao CHECK (data_eliminacao IS NULL OR data_eliminacao >= data_criacao),
    CONSTRAINT ck_servico_valores CHECK (valor_fixo_mensal >= 0 AND valor_por_membro >= 0),
    CONSTRAINT fk_servico_criador FOREIGN KEY (nif_criador) REFERENCES cliente (nif)
);

CREATE TABLE room_colab (
    id   INTEGER     NOT NULL,
    tipo VARCHAR(10) NOT NULL DEFAULT 'ROOM_COLAB',
    CONSTRAINT pk_room_colab PRIMARY KEY (id),
    CONSTRAINT ck_room_tipo CHECK (tipo = 'ROOM_COLAB'),
    CONSTRAINT fk_room_servico FOREIGN KEY (id, tipo)
        REFERENCES servico (id, tipo) ON DELETE CASCADE
);

CREATE TABLE text_chat (
    id   INTEGER     NOT NULL,
    tipo VARCHAR(10) NOT NULL DEFAULT 'TEXT_CHAT',
    CONSTRAINT pk_text_chat PRIMARY KEY (id),
    CONSTRAINT ck_chat_tipo CHECK (tipo = 'TEXT_CHAT'),
    CONSTRAINT fk_chat_servico FOREIGN KEY (id, tipo)
        REFERENCES servico (id, tipo) ON DELETE CASCADE
);

CREATE TABLE text_blog (
    id   INTEGER     NOT NULL,
    tipo VARCHAR(10) NOT NULL DEFAULT 'TEXT_BLOG',
    CONSTRAINT pk_text_blog PRIMARY KEY (id),
    CONSTRAINT ck_blog_tipo CHECK (tipo = 'TEXT_BLOG'),
    CONSTRAINT fk_blog_servico FOREIGN KEY (id, tipo)
        REFERENCES servico (id, tipo) ON DELETE CASCADE
);

-- ============================================================
-- ADERE: associacao de clientes a servicos por convite (N:M)
-- data_adesao NULL = convite ainda pendente
-- ============================================================
CREATE TABLE adere (
    id_servico    INTEGER NOT NULL,
    nif_cliente   BIGINT  NOT NULL,
    data_convite  DATE    NOT NULL,
    data_adesao   DATE,
    data_abandono DATE,
    CONSTRAINT pk_adere PRIMARY KEY (id_servico, nif_cliente),
    CONSTRAINT ck_adere_adesao  CHECK (data_adesao IS NULL OR data_adesao >= data_convite),
    CONSTRAINT ck_adere_abandono CHECK (data_abandono IS NULL
        OR (data_adesao IS NOT NULL AND data_abandono >= data_adesao)),
    CONSTRAINT fk_adere_servico FOREIGN KEY (id_servico)
        REFERENCES servico (id) ON DELETE CASCADE,
    CONSTRAINT fk_adere_cliente FOREIGN KEY (nif_cliente)
        REFERENCES cliente (nif)
);

-- ============================================================
-- SESSAO_COLAB (entidade fraca de ROOM_COLAB) e PARTICIPA
-- ============================================================
CREATE TABLE sessao_colab (
    id_room          INTEGER   NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim    TIMESTAMP,
    CONSTRAINT pk_sessao_colab PRIMARY KEY (id_room, data_hora_inicio),
    CONSTRAINT ck_sessao_fim CHECK (data_hora_fim IS NULL OR data_hora_fim > data_hora_inicio),
    CONSTRAINT fk_sessao_room FOREIGN KEY (id_room)
        REFERENCES room_colab (id) ON DELETE CASCADE
);

CREATE TABLE participa (
    id_room          INTEGER   NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    nif_cliente      BIGINT    NOT NULL,
    data_hora_entrada TIMESTAMP NOT NULL,
    data_hora_saida   TIMESTAMP,
    CONSTRAINT pk_participa PRIMARY KEY (id_room, data_hora_inicio, nif_cliente),
    CONSTRAINT ck_participa_saida CHECK (data_hora_saida IS NULL OR data_hora_saida >= data_hora_entrada),
    CONSTRAINT ck_participa_entrada CHECK (data_hora_entrada >= data_hora_inicio),
    CONSTRAINT fk_participa_sessao FOREIGN KEY (id_room, data_hora_inicio)
        REFERENCES sessao_colab (id_room, data_hora_inicio) ON DELETE CASCADE,
    CONSTRAINT fk_participa_cliente FOREIGN KEY (nif_cliente)
        REFERENCES cliente (nif)
);

-- ============================================================
-- TEXTO_CHAT / TEXTO_BLOG (entidades fracas dos respetivos servicos)
-- num_texto e atribuido por trigger (sequencial dentro do servico)
-- ============================================================
CREATE TABLE texto_chat (
    id_servico            INTEGER   NOT NULL,
    num_texto             INTEGER   NOT NULL,
    conteudo              TEXT      NOT NULL,
    data_hora_publicacao  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nif_autor             BIGINT    NOT NULL,
    CONSTRAINT pk_texto_chat PRIMARY KEY (id_servico, num_texto),
    CONSTRAINT fk_tchat_servico FOREIGN KEY (id_servico)
        REFERENCES text_chat (id) ON DELETE CASCADE,
    CONSTRAINT fk_tchat_autor FOREIGN KEY (nif_autor) REFERENCES cliente (nif)
);

CREATE TABLE texto_blog (
    id_servico            INTEGER   NOT NULL,
    num_texto             INTEGER   NOT NULL,
    titulo                VARCHAR(150) NOT NULL,
    conteudo              TEXT      NOT NULL,
    data_hora_publicacao  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nif_autor             BIGINT    NOT NULL,
    CONSTRAINT pk_texto_blog PRIMARY KEY (id_servico, num_texto),
    CONSTRAINT fk_tblog_servico FOREIGN KEY (id_servico)
        REFERENCES text_blog (id) ON DELETE CASCADE,
    CONSTRAINT fk_tblog_autor FOREIGN KEY (nif_autor) REFERENCES pessoa_singular (nif)
);

-- ============================================================
-- FATURA: um cliente, uma por mes de referencia
-- estado_pagamento regista o resultado da tentativa de pagamento automatico
-- ============================================================
CREATE TABLE fatura (
    numero            INTEGER GENERATED ALWAYS AS IDENTITY,
    nif_cliente       BIGINT        NOT NULL,
    id_metodo         INTEGER       NOT NULL,
    mes_referencia    DATE          NOT NULL,
    data_emissao      DATE          NOT NULL DEFAULT CURRENT_DATE,
    montante          NUMERIC(10,2) NOT NULL,
    estado_pagamento  VARCHAR(10)   NOT NULL DEFAULT 'PENDENTE',
    data_pagamento    TIMESTAMP,
    CONSTRAINT pk_fatura PRIMARY KEY (numero),
    CONSTRAINT uq_fatura_cliente_mes UNIQUE (nif_cliente, mes_referencia),
    CONSTRAINT ck_fatura_mes CHECK (EXTRACT(DAY FROM mes_referencia) = 1),
    CONSTRAINT ck_fatura_montante CHECK (montante > 0),
    CONSTRAINT ck_fatura_estado CHECK (estado_pagamento IN ('PENDENTE','PAGO','FALHADO')),
    CONSTRAINT ck_fatura_data_pag CHECK ((estado_pagamento = 'PAGO') = (data_pagamento IS NOT NULL)),
    CONSTRAINT fk_fatura_cliente FOREIGN KEY (nif_cliente) REFERENCES cliente (nif),
    CONSTRAINT fk_fatura_metodo FOREIGN KEY (id_metodo) REFERENCES metodo_pagamento (id)
);

-- ============================================================
-- MENSAGEM_AJUDA (entidade fraca de CLIENTE)
-- ============================================================
CREATE TABLE mensagem_ajuda (
    nif_cliente          BIGINT    NOT NULL,
    data_hora_pergunta   TIMESTAMP NOT NULL,
    texto_pergunta       TEXT      NOT NULL,
    texto_resposta       TEXT,
    data_hora_resposta   TIMESTAMP,
    num_empregado_resp   INTEGER,
    num_empregado_valida INTEGER,
    CONSTRAINT pk_mensagem_ajuda PRIMARY KEY (nif_cliente, data_hora_pergunta),
    CONSTRAINT ck_msg_resposta CHECK (
        (texto_resposta IS NULL AND data_hora_resposta IS NULL AND num_empregado_resp IS NULL)
        OR
        (texto_resposta IS NOT NULL AND data_hora_resposta IS NOT NULL AND num_empregado_resp IS NOT NULL)),
    CONSTRAINT ck_msg_datas CHECK (data_hora_resposta IS NULL OR data_hora_resposta >= data_hora_pergunta),
    CONSTRAINT ck_msg_valida CHECK (num_empregado_valida IS NULL
        OR (num_empregado_resp IS NOT NULL AND num_empregado_valida <> num_empregado_resp)),
    CONSTRAINT fk_msg_cliente FOREIGN KEY (nif_cliente) REFERENCES cliente (nif) ON DELETE CASCADE,
    CONSTRAINT fk_msg_resp FOREIGN KEY (num_empregado_resp) REFERENCES empregado (num_empregado),
    CONSTRAINT fk_msg_valida FOREIGN KEY (num_empregado_valida) REFERENCES empregado (num_empregado)
);

-- ============================================================
-- INDICES de apoio as interrogacoes mais frequentes
-- ============================================================
CREATE INDEX ix_servico_criador ON servico (nif_criador);
CREATE INDEX ix_adere_cliente   ON adere (nif_cliente);
CREATE INDEX ix_participa_cli   ON participa (nif_cliente);
CREATE INDEX ix_fatura_cliente  ON fatura (nif_cliente);

-- ============================================================
-- RESTRICOES NAO EXPRESSAS NO MODELO RELACIONAL (triggers)
-- ============================================================

-- Funcao auxiliar: o cliente e membro do servico? (criador ou aderente
-- que ja aderiu e ainda nao abandonou)
CREATE OR REPLACE FUNCTION e_membro(p_servico INTEGER, p_nif BIGINT)
RETURNS BOOLEAN AS $$
    SELECT EXISTS (SELECT 1 FROM servico s WHERE s.id = p_servico AND s.nif_criador = p_nif)
        OR EXISTS (SELECT 1 FROM adere a
                   WHERE a.id_servico = p_servico AND a.nif_cliente = p_nif
                     AND a.data_adesao IS NOT NULL);
$$ LANGUAGE sql STABLE;

-- 1) TEXT_BLOG: criador e membros so podem ser pessoas singulares;
--    o criador nao adere ao seu proprio servico; datas coerentes com o servico
CREATE OR REPLACE FUNCTION trg_adere_check() RETURNS TRIGGER AS $$
DECLARE s servico%ROWTYPE;
BEGIN
    SELECT * INTO s FROM servico WHERE id = NEW.id_servico;
    IF s.nif_criador = NEW.nif_cliente THEN
        RAISE EXCEPTION 'O criador (%) nao pode aderir ao proprio servico', NEW.nif_cliente;
    END IF;
    IF s.tipo = 'TEXT_BLOG'
       AND NOT EXISTS (SELECT 1 FROM pessoa_singular WHERE nif = NEW.nif_cliente) THEN
        RAISE EXCEPTION 'Um TEXT_BLOG so admite pessoas singulares como membros';
    END IF;
    IF NEW.data_convite < s.data_criacao THEN
        RAISE EXCEPTION 'Convite anterior a data de criacao do servico';
    END IF;
    IF s.data_eliminacao IS NOT NULL AND
       (NEW.data_convite > s.data_eliminacao
        OR COALESCE(NEW.data_adesao, s.data_eliminacao) > s.data_eliminacao
        OR COALESCE(NEW.data_abandono, s.data_eliminacao) > s.data_eliminacao) THEN
        RAISE EXCEPTION 'Datas posteriores a eliminacao do servico %', s.id;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER t_adere_check BEFORE INSERT OR UPDATE ON adere
    FOR EACH ROW EXECUTE FUNCTION trg_adere_check();

CREATE OR REPLACE FUNCTION trg_servico_blog_criador() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.tipo = 'TEXT_BLOG'
       AND NOT EXISTS (SELECT 1 FROM pessoa_singular WHERE nif = NEW.nif_criador) THEN
        RAISE EXCEPTION 'Um TEXT_BLOG so pode ser criado por uma pessoa singular (NIF %)', NEW.nif_criador;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER t_servico_blog_criador BEFORE INSERT OR UPDATE ON servico
    FOR EACH ROW EXECUTE FUNCTION trg_servico_blog_criador();

-- 2) Participantes de sessoes e autores de textos tem de ser membros do servico
CREATE OR REPLACE FUNCTION trg_participa_membro() RETURNS TRIGGER AS $$
DECLARE d DATE;
BEGIN
    IF NOT e_membro(NEW.id_room, NEW.nif_cliente) THEN
        RAISE EXCEPTION 'Cliente % nao e membro do servico %', NEW.nif_cliente, NEW.id_room;
    END IF;
    SELECT data_eliminacao INTO d FROM servico WHERE id = NEW.id_room;
    IF d IS NOT NULL AND NEW.data_hora_entrada::date > d THEN
        RAISE EXCEPTION 'Servico % ja eliminado', NEW.id_room;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER t_participa_membro BEFORE INSERT OR UPDATE ON participa
    FOR EACH ROW EXECUTE FUNCTION trg_participa_membro();

CREATE OR REPLACE FUNCTION trg_texto_membro() RETURNS TRIGGER AS $$
DECLARE d DATE;
BEGIN
    IF NOT e_membro(NEW.id_servico, NEW.nif_autor) THEN
        RAISE EXCEPTION 'Cliente % nao e membro do servico %', NEW.nif_autor, NEW.id_servico;
    END IF;
    SELECT data_eliminacao INTO d FROM servico WHERE id = NEW.id_servico;
    IF d IS NOT NULL AND NEW.data_hora_publicacao::date > d THEN
        RAISE EXCEPTION 'Servico % ja eliminado', NEW.id_servico;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER t_tchat_membro BEFORE INSERT OR UPDATE ON texto_chat
    FOR EACH ROW EXECUTE FUNCTION trg_texto_membro();
CREATE TRIGGER t_tblog_membro BEFORE INSERT OR UPDATE ON texto_blog
    FOR EACH ROW EXECUTE FUNCTION trg_texto_membro();

-- 3) Numeracao sequencial dos textos dentro de cada servico (chave parcial)
CREATE OR REPLACE FUNCTION trg_num_texto() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.num_texto IS NULL THEN
        IF TG_TABLE_NAME = 'texto_chat' THEN
            SELECT COALESCE(MAX(num_texto),0)+1 INTO NEW.num_texto
              FROM texto_chat WHERE id_servico = NEW.id_servico;
        ELSE
            SELECT COALESCE(MAX(num_texto),0)+1 INTO NEW.num_texto
              FROM texto_blog WHERE id_servico = NEW.id_servico;
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER t_tchat_num BEFORE INSERT ON texto_chat
    FOR EACH ROW EXECUTE FUNCTION trg_num_texto();
CREATE TRIGGER t_tblog_num BEFORE INSERT ON texto_blog
    FOR EACH ROW EXECUTE FUNCTION trg_num_texto();

-- 4) Fatura: empregados estao isentos; o metodo de pagamento e do cliente faturado
CREATE OR REPLACE FUNCTION trg_fatura_check() RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM empregado WHERE nif = NEW.nif_cliente) THEN
        RAISE EXCEPTION 'Empregados (NIF %) estao isentos de pagamento', NEW.nif_cliente;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM metodo_pagamento
                   WHERE id = NEW.id_metodo AND nif_cliente = NEW.nif_cliente) THEN
        RAISE EXCEPTION 'O metodo de pagamento % nao pertence ao cliente %', NEW.id_metodo, NEW.nif_cliente;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;
CREATE TRIGGER t_fatura_check BEFORE INSERT OR UPDATE ON fatura
    FOR EACH ROW EXECUTE FUNCTION trg_fatura_check();

-- ============================================================
-- VISTAS auxiliares
-- ============================================================
-- Numero de membros aderentes ativos por servico (nao inclui o criador)
CREATE OR REPLACE VIEW v_membros_servico AS
SELECT s.id AS id_servico,
       COUNT(a.nif_cliente) FILTER (WHERE a.data_adesao IS NOT NULL
                                      AND a.data_abandono IS NULL) AS num_membros
FROM servico s
LEFT JOIN adere a ON a.id_servico = s.id
GROUP BY s.id;
