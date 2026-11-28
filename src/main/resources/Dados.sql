-- ============================================================
-- EUColab / ISysHolCol - estado de validacao (dados de teste)
-- Executar depois de criar_tabelas.sql
-- ============================================================
SET search_path TO holcol;

BEGIN;

-- CODIGO_POSTAL
INSERT INTO codigo_postal (codigo_postal, pais, cidade) VALUES
('1000-021', 'Portugal', 'Lisboa'),
('1500-001', 'Portugal', 'Lisboa'),
('4000-001', 'Portugal', 'Porto'),
('3000-001', 'Portugal', 'Coimbra'),
('75001',    'França',   'Paris');

-- CLIENTE (S = pessoa singular, C = pessoa coletiva)
INSERT INTO cliente (nif, tipo, email, rua, num_porta, andar, letra, codigo_postal, pais) VALUES
(111111111, 'S', 'ana.silva@mail.pt',      'Rua Augusta',            '10',  '2', 'A', '1000-021', 'Portugal'),
(222222222, 'S', 'bruno.costa@mail.pt',    'Rua de Santa Catarina',  '55',  NULL, NULL, '4000-001', 'Portugal'),
(333333333, 'S', 'carla.mendes@mail.pt',   'Avenida de Roma',        '8',   '5', 'D', '1500-001', 'Portugal'),
(444444444, 'S', 'diogo.ferreira@mail.pt', 'Rua Ferreira Borges',    '120', '1', 'B', '3000-001', 'Portugal'),
(555555555, 'S', 'eva.martins@mail.fr',    'Rue de Rivoli',          '21',  '3', NULL, '75001',    'França'),
(666666666, 'S', 'hugo.lopes@eucolab.eu',  'Rua do Ouro',            '99',  NULL, NULL, '1000-021', 'Portugal'),
(777777777, 'S', 'rita.pinto@eucolab.eu',  'Avenida da Liberdade',   '200', '4', 'E', '1500-001', 'Portugal'),
(500000001, 'C', 'geral@isel.pt',          'Rua Conselheiro Emídio Navarro', '1', NULL, NULL, '1000-021', 'Portugal'),
(500000002, 'C', 'info@techporto.pt',      'Rua Sá da Bandeira',     '300', '2', 'C', '4000-001', 'Portugal');

INSERT INTO pessoa_singular (nif, nome_proprio, apelido, telemovel, data_nascimento) VALUES
(111111111, 'Ana',   'Silva',    '912345678', '1998-04-05'),
(222222222, 'Bruno', 'Costa',    '923456789', '1995-09-12'),
(333333333, 'Carla', 'Mendes',   '934567890', '2000-01-30'),
(444444444, 'Diogo', 'Ferreira', '965432109', '1992-11-23'),
(555555555, 'Eva',   'Martins',  '916543210', '1988-07-17'),
(666666666, 'Hugo',  'Lopes',    '917654321', '1985-02-10'),
(777777777, 'Rita',  'Pinto',    '918765432', '1980-06-01');

INSERT INTO pessoa_coletiva (nif, designacao) VALUES
(500000001, 'ISEL - Instituto Superior de Engenharia de Lisboa'),
(500000002, 'Tech Porto Lda');

INSERT INTO contacto (nif_coletiva, num_contacto, nome, text_chat) VALUES
(500000001, 1, 'Secretariado DEETC', '@secretariado.deetc'),
(500000001, 2, 'Luís Osório',        '@luis.osorio'),
(500000002, 1, 'Marta Ribeiro',      '@marta.techporto');

-- EMPREGADO (pessoas singulares que sao tambem clientes)
INSERT INTO empregado (num_empregado, nif, data_admissao) VALUES
(1, 777777777, '2015-04-20'),
(2, 666666666, '2018-03-10');

-- METODO_PAGAMENTO (ids explicitos; as sequencias sao acertadas no fim)
INSERT INTO metodo_pagamento (id, nif_cliente, tipo, por_omissao) OVERRIDING SYSTEM VALUE VALUES
(1, 111111111, 'MBWAY',  TRUE),
(2, 111111111, 'CARTAO', FALSE),
(3, 222222222, 'CARTAO', TRUE),
(4, 333333333, 'MBWAY',  TRUE),
(5, 444444444, 'MBWAY',  TRUE),
(6, 500000001, 'CARTAO', TRUE),
(7, 500000002, 'CARTAO', TRUE),
(8, 555555555, 'MBWAY',  TRUE);

INSERT INTO mbway (id, telemovel) VALUES
(1, '912345678'), (4, '934567890'), (5, '965432109'), (8, '0033612345678');

INSERT INTO cartao_credito (id, nome_cartao, numero, data_validade, codigo) VALUES
(2, 'ANA SILVA',         '4111111111111111', '2028-05-31', '123'),
(3, 'BRUNO COSTA',       '5500000000000004', '2027-11-30', '456'),
(6, 'ISEL',              '4222222222222222', '2029-01-31', '789'),
(7, 'TECH PORTO LDA',    '4333333333333333', '2027-08-31', '321');

-- SERVICO
INSERT INTO servico (id, tipo, designacao, nif_criador, data_criacao, data_eliminacao,
                     modalidade, valor_fixo_mensal, valor_por_membro) OVERRIDING SYSTEM VALUE VALUES
(1, 'ROOM_COLAB', 'Reunião Semanal ISEL', 500000001, '2026-01-10', NULL,         'FIXO',     20.00, 0),
(2, 'TEXT_CHAT',  'Chat Projeto SI',      111111111, '2026-02-01', NULL,         'VARIAVEL',  0,    2.50),
(3, 'TEXT_BLOG',  'Blog de Viagens',      222222222, '2026-03-05', NULL,         'FIXO',      5.00, 0),
(4, 'ROOM_COLAB', 'Sala Aulas Redes',     111111111, '2026-02-15', NULL,         'VARIAVEL',  0,    3.00),
(5, 'TEXT_CHAT',  'Suporte Tech Porto',   500000002, '2026-01-20', '2026-09-30', 'FIXO',     10.00, 0),
(6, 'TEXT_BLOG',  'Diário de Estudo',     333333333, '2026-04-01', NULL,         'FIXO',      4.00, 0),
(7, 'ROOM_COLAB', 'Sala Aberta Diogo',    444444444, '2026-05-01', NULL,         'FIXO',      0,    0);

INSERT INTO room_colab (id) VALUES (1), (4), (7);
INSERT INTO text_chat  (id) VALUES (2), (5);
INSERT INTO text_blog  (id) VALUES (3), (6);

-- ADERE (convite, adesao, abandono)
INSERT INTO adere (id_servico, nif_cliente, data_convite, data_adesao, data_abandono) VALUES
-- servico 1
(1, 111111111, '2026-01-12', '2026-01-13', NULL),
(1, 222222222, '2026-01-12', '2026-01-15', NULL),
(1, 333333333, '2026-01-12', '2026-01-14', NULL),
(1, 444444444, '2026-01-20', '2026-01-21', NULL),
(1, 555555555, '2026-01-20', '2026-01-22', '2026-08-01'),
-- servico 2
(2, 222222222, '2026-02-02', '2026-02-03', NULL),
(2, 333333333, '2026-02-02', '2026-02-04', NULL),
(2, 444444444, '2026-02-05', '2026-02-06', NULL),
(2, 555555555, '2026-02-05', '2026-02-10', NULL),
(2, 500000001, '2026-02-10', '2026-02-11', NULL),
-- servico 3 (blog: so pessoas singulares)
(3, 111111111, '2026-03-06', '2026-03-07', NULL),
(3, 333333333, '2026-03-06', '2026-03-08', NULL),
(3, 555555555, '2026-03-10', NULL,         NULL),
-- servico 4
(4, 222222222, '2026-02-16', '2026-02-17', NULL),
(4, 333333333, '2026-02-16', '2026-02-18', NULL),
(4, 444444444, '2026-02-20', '2026-02-21', NULL),
(4, 555555555, '2026-02-20', '2026-02-22', NULL),
-- servico 5 (eliminado em 2026-09-30)
(5, 111111111, '2026-01-21', '2026-01-22', NULL),
(5, 222222222, '2026-01-21', '2026-01-23', NULL),
(5, 333333333, '2026-01-25', '2026-01-26', NULL),
(5, 444444444, '2026-01-25', '2026-01-26', '2026-06-01'),
-- servico 6
(6, 111111111, '2026-04-02', '2026-04-03', NULL),
-- servico 7
(7, 555555555, '2026-05-02', '2026-05-03', NULL),
(7, 222222222, '2026-05-02', '2026-05-04', NULL);

-- SESSAO_COLAB
INSERT INTO sessao_colab (id_room, data_hora_inicio, data_hora_fim) VALUES
(1, '2026-09-01 10:00', '2026-09-01 11:00'),
(1, '2026-09-08 10:00', '2026-09-08 11:00'),
(1, '2026-10-06 10:00', '2026-10-06 11:30'),
(4, '2026-10-10 14:00', '2026-10-10 15:00'),
(7, '2026-06-01 09:00', '2026-06-01 09:30');

-- PARTICIPA
INSERT INTO participa (id_room, data_hora_inicio, nif_cliente, data_hora_entrada, data_hora_saida) VALUES
(1, '2026-09-01 10:00', 500000001, '2026-09-01 10:00', '2026-09-01 11:00'),
(1, '2026-09-01 10:00', 111111111, '2026-09-01 10:02', '2026-09-01 11:00'),
(1, '2026-09-01 10:00', 222222222, '2026-09-01 10:05', '2026-09-01 10:50'),
(1, '2026-09-01 10:00', 333333333, '2026-09-01 10:00', '2026-09-01 11:00'),
(1, '2026-09-08 10:00', 500000001, '2026-09-08 10:00', '2026-09-08 11:00'),
(1, '2026-09-08 10:00', 111111111, '2026-09-08 10:01', '2026-09-08 11:00'),
(1, '2026-09-08 10:00', 222222222, '2026-09-08 10:03', '2026-09-08 11:00'),
(1, '2026-09-08 10:00', 333333333, '2026-09-08 10:00', '2026-09-08 10:40'),
(1, '2026-10-06 10:00', 500000001, '2026-10-06 10:00', '2026-10-06 11:30'),
(1, '2026-10-06 10:00', 111111111, '2026-10-06 10:00', '2026-10-06 11:30'),
(1, '2026-10-06 10:00', 222222222, '2026-10-06 10:04', '2026-10-06 11:30'),
(1, '2026-10-06 10:00', 333333333, '2026-10-06 10:00', '2026-10-06 11:30'),
(1, '2026-10-06 10:00', 444444444, '2026-10-06 10:10', '2026-10-06 11:00'),
(4, '2026-10-10 14:00', 111111111, '2026-10-10 14:00', '2026-10-10 15:00'),
(4, '2026-10-10 14:00', 222222222, '2026-10-10 14:02', '2026-10-10 15:00'),
(4, '2026-10-10 14:00', 333333333, '2026-10-10 14:05', NULL),
(7, '2026-06-01 09:00', 444444444, '2026-06-01 09:00', '2026-06-01 09:30'),
(7, '2026-06-01 09:00', 555555555, '2026-06-01 09:05', '2026-06-01 09:30');

-- TEXTO_CHAT
INSERT INTO texto_chat (id_servico, num_texto, conteudo, data_hora_publicacao, nif_autor) VALUES
(2, 1, 'Bom dia a todos, vamos definir as tarefas da fase 2.', '2026-03-02 09:00', 111111111),
(2, 2, 'Eu trato do modelo físico.',                            '2026-03-02 09:15', 222222222),
(2, 3, 'Eu fico com a aplicação em Java.',                      '2026-03-02 09:20', 333333333),
(2, 4, 'A ISEL disponibiliza a sala para a reunião final.',     '2026-03-03 11:00', 500000001),
(5, 1, 'Pedido de suporte aberto para o cliente 12.',           '2026-02-10 14:00', 500000002),
(5, 2, 'Recebido, trato disto hoje.',                           '2026-02-10 14:05', 111111111);

-- TEXTO_BLOG
INSERT INTO texto_blog (id_servico, num_texto, titulo, conteudo, data_hora_publicacao, nif_autor) VALUES
(3, 1, 'Três dias em Lisboa',  'Notas de viagem sobre Lisboa...',    '2026-04-01 18:00', 222222222),
(3, 2, 'Comentários sobre Lisboa', 'Concordo e acrescento...',        '2026-04-02 20:30', 111111111),
(3, 3, 'Roteiro do Porto',     'O que visitar no Porto...',          '2026-05-10 19:00', 222222222),
(6, 1, 'Resumo de SI',         'Resumo das aulas de Sistemas de Informação.', '2026-04-20 21:00', 333333333),
(6, 2, 'Dicas de estudo',      'Planeamento semanal e revisão ativa.', '2026-05-02 08:00', 111111111);

-- FATURA (ids explicitos; mes_referencia = 1.o dia do mes faturado)
INSERT INTO fatura (numero, nif_cliente, id_metodo, mes_referencia, data_emissao, montante,
                    estado_pagamento, data_pagamento) OVERRIDING SYSTEM VALUE VALUES
(1, 500000001, 6, '2026-09-01', '2026-10-01', 20.00, 'PAGO',    '2026-10-01 03:00'),
(2, 500000002, 7, '2026-09-01', '2026-10-01', 10.00, 'FALHADO', NULL),
(3, 500000001, 6, '2026-10-01', '2026-11-01', 20.00, 'PAGO',    '2026-11-01 03:00'),
(4, 111111111, 1, '2026-10-01', '2026-11-01',  8.00, 'PENDENTE', NULL);

-- MENSAGEM_AJUDA
INSERT INTO mensagem_ajuda (nif_cliente, data_hora_pergunta, texto_pergunta,
                            texto_resposta, data_hora_resposta,
                            num_empregado_resp, num_empregado_valida) VALUES
(111111111, '2026-10-02 09:30', 'Como convido um cliente para o meu serviço?',
            'Em Serviços > Convidar, indique o NIF do cliente.', '2026-10-02 11:00', 2, 1),
(222222222, '2026-10-15 16:45', 'Porque razão a minha fatura veio com um valor diferente?',
            'O valor depende do número de membros aderentes acima de três.', '2026-10-16 09:10', 1, NULL),
(500000001, '2026-11-10 08:20', 'Podemos alterar o método de pagamento por omissão?', NULL, NULL, NULL, NULL);

-- Acertar as sequencias das colunas identity depois das insercoes com ids explicitos
SELECT setval(pg_get_serial_sequence('holcol.metodo_pagamento','id'), (SELECT MAX(id) FROM metodo_pagamento));
SELECT setval(pg_get_serial_sequence('holcol.servico','id'),           (SELECT MAX(id) FROM servico));
SELECT setval(pg_get_serial_sequence('holcol.fatura','numero'),        (SELECT MAX(numero) FROM fatura));

COMMIT;
