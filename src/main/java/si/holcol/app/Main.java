package si.holcol.app;


import si.holcol.dao.AjudaDAO;
import si.holcol.dao.AtividadeDAO;
import si.holcol.dao.ClienteDAO;
import si.holcol.dao.ClienteDAO.Contacto; // <-- Adicionado para expor o registo aninhado
import si.holcol.dao.ClienteDAO.Morada;   // <-- Adicionado para expor o registo aninhado
import si.holcol.dao.FaturacaoDAO;
import si.holcol.dao.ServicoDAO;
import si.holcol.db.ConnectionFactory;
import si.holcol.db.Tx;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** HolColApp - interface em linha de comandos do sistema ISysHolCol (EUColab). */
public class Main {

    private static final Scanner IN = new Scanner(System.in);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final ClienteDAO clientes = new ClienteDAO();
    private static final ServicoDAO servicos = new ServicoDAO();
    private static final AtividadeDAO atividade = new AtividadeDAO();
    private static final FaturacaoDAO faturacao = new FaturacaoDAO();
    private static final AjudaDAO ajuda = new AjudaDAO();

    /** Operacao de menu que pode falhar com SQLException ou com dados invalidos. */
    @FunctionalInterface
    private interface Acao {
        void executar(Connection c) throws SQLException;
    }

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== HolColApp (EUColab) ===");
            System.out.println("1. Clientes e metodos de pagamento");
            System.out.println("2. Servicos e membros");
            System.out.println("3. Atividade de colaboracao");
            System.out.println("4. Faturacao mensal");
            System.out.println("5. Pedidos de ajuda");
            System.out.println("0. Sair");
            switch (lerInt("Opcao")) {
                case 1 -> menuClientes();
                case 2 -> menuServicos();
                case 3 -> menuAtividade();
                case 4 -> menuFaturacao();
                case 5 -> menuAjuda();
                case 0 -> { System.out.println("Fim."); return; }
                default -> System.out.println("Opcao invalida");
            }
        }
    }

    // ------------------------------------------------------------------
    // Execucao: uma ligacao por operacao, fechada no fim (try-with-resources)
    // ------------------------------------------------------------------
    private static void executar(Acao acao) {
        try (Connection c = ConnectionFactory.getConnection()) {
            acao.executar(c);
        } catch (SQLException e) {
            System.out.println("Erro de base de dados: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Operacao recusada: " + e.getMessage());
        }
    }

    /** Operacao atomica: tudo ou nada. */
    private static void transacao(Acao acao) {
        executar(c -> Tx.run(c, conn -> { acao.executar(conn); return null; }));
    }

    // ------------------------------------------------------------------
    // 1. Clientes
    // ------------------------------------------------------------------
    private static void menuClientes() {
        System.out.println("\n-- Clientes --");
        System.out.println("1. Registar pessoa singular");
        System.out.println("2. Registar pessoa coletiva");
        System.out.println("3. Adicionar metodo de pagamento");
        System.out.println("4. Definir metodo por omissao");
        System.out.println("5. Listar metodos de um cliente");
        switch (lerInt("Opcao")) {
            case 1 -> registarSingular();
            case 2 -> registarColetiva();
            case 3 -> adicionarMetodo();
            case 4 -> {
                long nif = lerLong("NIF do cliente");
                int id = lerInt("Id do metodo");
                transacao(c -> { clientes.definirPorOmissao(c, nif, id); System.out.println("Metodo por omissao atualizado."); });
            }
            case 5 -> { long nif = lerLong("NIF do cliente"); executar(c -> clientes.listarMetodos(c, nif)); }
            default -> { }
        }
    }

    private static Morada lerMorada() {
        String rua = ler("Rua");
        String porta = ler("Numero da porta");
        String andar = lerOpcional("Andar (enter se nao tiver)");
        String letra = lerOpcional("Letra (enter se nao tiver)");
        String cp = ler("Codigo postal");
        String cidade = ler("Cidade");
        String pais = ler("Pais");
        return new Morada(rua, porta, andar, letra, cp, cidade, pais);
    }

    private static void registarSingular() {
        long nif = lerLong("NIF");
        String email = ler("Email");
        String nome = ler("Nome proprio");
        String apelido = ler("Apelido");
        String tel = ler("Telemovel");
        LocalDate nasc = lerData("Data de nascimento (yyyy-MM-dd)");
        Morada m = lerMorada();
        // cliente + morada + pessoa singular (+ metodos) numa unica transacao
        transacao(c -> {
            clientes.registarSingular(c, nif, email, m, nome, apelido, tel, nasc);
            lerMetodosPagamento(c, nif);
            System.out.println("Cliente registado.");
        });
    }

    private static void registarColetiva() {
        long nif = lerLong("NIF");
        String email = ler("Email");
        String designacao = ler("Designacao");
        Morada m = lerMorada();
        List<Contacto> contactos = new ArrayList<>();
        while (contactos.size() < 5 && lerSimNao("Adicionar pessoa de contacto? (" + contactos.size() + "/5)")) {
            contactos.add(new Contacto(ler("Nome do contacto"), ler("TextChat do contacto")));
        }
        transacao(c -> {
            clientes.registarColetiva(c, nif, email, m, designacao, contactos);
            lerMetodosPagamento(c, nif);
            System.out.println("Cliente registado.");
        });
    }

    /** Pergunta e regista metodos de pagamento do cliente (o primeiro fica por omissao). */
    private static void lerMetodosPagamento(Connection c, long nif) throws SQLException {
        while (lerSimNao("Adicionar metodo de pagamento?")) {
            inserirMetodo(c, nif);
        }
    }

    private static void adicionarMetodo() {
        long nif = lerLong("NIF do cliente");
        transacao(c -> inserirMetodo(c, nif));
    }

    private static void inserirMetodo(Connection c, long nif) throws SQLException {
        String tipo = ler("Tipo (MBWAY/CARTAO)").toUpperCase();
        boolean omissao = lerSimNao("Definir como metodo por omissao?");
        int id;
        if (tipo.equals("MBWAY")) {
            id = clientes.registarMBWay(c, nif, ler("Telemovel MB Way"), omissao);
        } else if (tipo.equals("CARTAO")) {
            id = clientes.registarCartao(c, nif, ler("Nome no cartao"), ler("Numero"),
                    lerMes("Validade (yyyy-MM)"), ler("Codigo"), omissao);
        } else {
            throw new IllegalArgumentException("Tipo de metodo invalido");
        }
        System.out.println("Metodo registado com id " + id);
    }

    // ------------------------------------------------------------------
    // 2. Servicos
    // ------------------------------------------------------------------
    private static void menuServicos() {
        System.out.println("\n-- Servicos --");
        System.out.println("1. Criar servico");
        System.out.println("2. Convidar cliente");
        System.out.println("3. Registar adesao");
        System.out.println("4. Registar abandono");
        System.out.println("5. Eliminar servico (criador)");
        System.out.println("6. Listar membros");
        switch (lerInt("Opcao")) {
            case 1 -> {
                String tipo = ler("Tipo (ROOM_COLAB/TEXT_CHAT/TEXT_BLOG)").toUpperCase();
                String des = ler("Designacao");
                long criador = lerLong("NIF do criador");
                LocalDate data = lerData("Data de criacao (yyyy-MM-dd)");
                String mod = ler("Modalidade (FIXO/VARIAVEL)").toUpperCase();
                BigDecimal fixo = lerDecimal("Valor fixo mensal");
                BigDecimal membro = lerDecimal("Valor por membro (acima de 3)");
                transacao(c -> System.out.println("Servico criado com id "
                        + servicos.criar(c, tipo, des, criador, data, mod, fixo, membro)));
            }
            case 2 -> {
                int id = lerInt("Id do servico"); long nif = lerLong("NIF a convidar");
                LocalDate d = lerData("Data do convite (yyyy-MM-dd)");
                transacao(c -> { servicos.convidar(c, id, nif, d); System.out.println("Convite registado."); });
            }
            case 3 -> {
                int id = lerInt("Id do servico"); long nif = lerLong("NIF");
                LocalDate d = lerData("Data de adesao (yyyy-MM-dd)");
                transacao(c -> { servicos.aderir(c, id, nif, d); System.out.println("Adesao registada."); });
            }
            case 4 -> {
                int id = lerInt("Id do servico"); long nif = lerLong("NIF");
                LocalDate d = lerData("Data de abandono (yyyy-MM-dd)");
                transacao(c -> { servicos.abandonar(c, id, nif, d); System.out.println("Abandono registado."); });
            }
            case 5 -> {
                int id = lerInt("Id do servico"); long nif = lerLong("NIF do criador");
                LocalDate d = lerData("Data de eliminacao (yyyy-MM-dd)");
                transacao(c -> { servicos.eliminar(c, id, nif, d); System.out.println("Servico eliminado."); });
            }
            case 6 -> { int id = lerInt("Id do servico"); executar(c -> servicos.listarMembros(c, id)); }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // 3. Atividade
    // ------------------------------------------------------------------
    private static void menuAtividade() {
        System.out.println("\n-- Atividade --");
        System.out.println("1. Iniciar sessao (ROOM_COLAB)");
        System.out.println("2. Entrada de participante");
        System.out.println("3. Saida de participante");
        System.out.println("4. Encerrar sessao");
        System.out.println("5. Publicar texto em TEXT_CHAT");
        System.out.println("6. Publicar texto em TEXT_BLOG");
        System.out.println("7. Listar sessoes de uma sala");
        System.out.println("8. Listar textos de um servico");
        switch (lerInt("Opcao")) {
            case 1 -> {
                int room = lerInt("Id da ROOM_COLAB"); LocalDateTime ini = lerDataHora("Inicio");
                transacao(c -> { atividade.iniciarSessao(c, room, ini); System.out.println("Sessao iniciada."); });
            }
            case 2 -> {
                int room = lerInt("Id da ROOM_COLAB"); LocalDateTime ini = lerDataHora("Inicio da sessao");
                long nif = lerLong("NIF do participante"); LocalDateTime ent = lerDataHora("Entrada");
                transacao(c -> { atividade.entrar(c, room, ini, nif, ent); System.out.println("Entrada registada."); });
            }
            case 3 -> {
                int room = lerInt("Id da ROOM_COLAB"); LocalDateTime ini = lerDataHora("Inicio da sessao");
                long nif = lerLong("NIF do participante"); LocalDateTime sai = lerDataHora("Saida");
                transacao(c -> { atividade.sair(c, room, ini, nif, sai); System.out.println("Saida registada."); });
            }
            case 4 -> {
                int room = lerInt("Id da ROOM_COLAB"); LocalDateTime ini = lerDataHora("Inicio da sessao");
                LocalDateTime fim = lerDataHora("Encerramento");
                transacao(c -> { atividade.encerrarSessao(c, room, ini, fim); System.out.println("Sessao encerrada."); });
            }
            case 5 -> {
                int id = lerInt("Id do TEXT_CHAT"); long nif = lerLong("NIF do autor");
                String txt = ler("Texto"); LocalDateTime dh = lerDataHoraOuAgora();
                transacao(c -> System.out.println("Texto publicado com numero " + atividade.publicarChat(c, id, nif, txt, dh)));
            }
            case 6 -> {
                int id = lerInt("Id do TEXT_BLOG"); long nif = lerLong("NIF do autor");
                String titulo = ler("Titulo"); String txt = ler("Texto"); LocalDateTime dh = lerDataHoraOuAgora();
                transacao(c -> System.out.println("Texto publicado com numero " + atividade.publicarBlog(c, id, nif, titulo, txt, dh)));
            }
            case 7 -> { int room = lerInt("Id da ROOM_COLAB"); executar(c -> atividade.listarSessoes(c, room)); }
            case 8 -> { int id = lerInt("Id do servico"); executar(c -> atividade.listarTextos(c, id)); }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // 4. Faturacao
    // ------------------------------------------------------------------
    private static void menuFaturacao() {
        System.out.println("\n-- Faturacao --");
        System.out.println("1. Apurar custo do mes (sem emitir)");
        System.out.println("2. Emitir fatura mensal");
        System.out.println("3. Tentar pagamento automatico de uma fatura");
        System.out.println("4. Listar faturas de um cliente");
        switch (lerInt("Opcao")) {
            case 1 -> {
                long nif = lerLong("NIF do cliente"); YearMonth mes = lerMes("Mes (yyyy-MM)");
                executar(c -> System.out.println("Custo apurado: " + faturacao.calcularMontante(c, nif, mes)));
            }
            case 2 -> {
                long nif = lerLong("NIF do cliente"); YearMonth mes = lerMes("Mes (yyyy-MM)");
                transacao(c -> {
                    FaturacaoDAO.Resultado r = faturacao.emitirFatura(c, nif, mes);
                    System.out.println(r.motivo() + (r.emitida()
                            ? " | fatura " + r.numeroFatura() + " | montante " + r.montante() : ""));
                });
            }
            case 3 -> {
                int numero = lerInt("Numero da fatura");
                transacao(c -> System.out.println(faturacao.tentarPagamento(c, numero)
                        ? "Pagamento aceite: fatura PAGA." : "Pagamento recusado: fatura FALHADA."));
            }
            case 4 -> { long nif = lerLong("NIF do cliente"); executar(c -> faturacao.listarFaturas(c, nif)); }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // 5. Ajuda
    // ------------------------------------------------------------------
    private static void menuAjuda() {
        System.out.println("\n-- Pedidos de ajuda --");
        System.out.println("1. Registar pedido");
        System.out.println("2. Responder (empregado)");
        System.out.println("3. Validar resposta (chefia)");
        System.out.println("4. Listar pedidos de um cliente");
        switch (lerInt("Opcao")) {
            case 1 -> {
                long nif = lerLong("NIF do cliente"); String txt = ler("Questao");
                LocalDateTime dh = lerDataHoraOuAgora();
                transacao(c -> { ajuda.registarPedido(c, nif, dh, txt); System.out.println("Pedido registado."); });
            }
            case 2 -> {
                long nif = lerLong("NIF do cliente"); LocalDateTime pergunta = lerDataHora("Data/hora do pedido");
                int emp = lerInt("Numero do empregado"); String txt = ler("Esclarecimento");
                LocalDateTime dh = lerDataHoraOuAgora();
                transacao(c -> { ajuda.responder(c, nif, pergunta, emp, txt, dh); System.out.println("Resposta registada."); });
            }
            case 3 -> {
                long nif = lerLong("NIF do cliente"); LocalDateTime pergunta = lerDataHora("Data/hora do pedido");
                int chefia = lerInt("Numero do empregado (chefia)");
                transacao(c -> { ajuda.validar(c, nif, pergunta, chefia); System.out.println("Resposta validada."); });
            }
            case 4 -> { long nif = lerLong("NIF do cliente"); executar(c -> ajuda.listar(c, nif)); }
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // Leitura de dados (repete ate o formato ser valido)
    // ------------------------------------------------------------------
    private static String ler(String pergunta) {
        while (true) {
            System.out.print(pergunta + ": ");
            String s = IN.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Valor obrigatorio.");
        }
    }

    private static String lerOpcional(String pergunta) {
        System.out.print(pergunta + ": ");
        String s = IN.nextLine().trim();
        return s.isEmpty() ? null : s;
    }

    private static int lerInt(String pergunta) {
        while (true) {
            try { return Integer.parseInt(ler(pergunta)); }
            catch (NumberFormatException e) { System.out.println("Numero invalido."); }
        }
    }

    private static long lerLong(String pergunta) {
        while (true) {
            try { return Long.parseLong(ler(pergunta)); }
            catch (NumberFormatException e) { System.out.println("Numero invalido."); }
        }
    }

    private static BigDecimal lerDecimal(String pergunta) {
        while (true) {
            try { return new BigDecimal(ler(pergunta).replace(',', '.')); }
            catch (NumberFormatException e) { System.out.println("Valor invalido."); }
        }
    }

    private static LocalDate lerData(String pergunta) {
        while (true) {
            try { return LocalDate.parse(ler(pergunta)); }
            catch (DateTimeParseException e) { System.out.println("Data invalida (yyyy-MM-dd)."); }
        }
    }

    private static YearMonth lerMes(String pergunta) {
        while (true) {
            try { return YearMonth.parse(ler(pergunta)); }
            catch (DateTimeParseException e) { System.out.println("Mes invalido (yyyy-MM)."); }
        }
    }

    private static LocalDateTime lerDataHora(String pergunta) {
        while (true) {
            try { return LocalDateTime.parse(ler(pergunta + " (yyyy-MM-dd HH:mm)"), DATA_HORA); }
            catch (DateTimeParseException e) { System.out.println("Data/hora invalida."); }
        }
    }

    private static LocalDateTime lerDataHoraOuAgora() {
        String s = lerOpcional("Data/hora (yyyy-MM-dd HH:mm, enter = agora)");
        if (s == null) return LocalDateTime.now().withSecond(0).withNano(0);
        try { return LocalDateTime.parse(s, DATA_HORA); }
        catch (DateTimeParseException e) {
            System.out.println("Formato invalido, a usar a data/hora atual.");
            return LocalDateTime.now().withSecond(0).withNano(0);
        }
    }

    private static boolean lerSimNao(String pergunta) {
        System.out.print(pergunta + " [s/N]: ");
        return IN.nextLine().trim().equalsIgnoreCase("s");
    }
}
