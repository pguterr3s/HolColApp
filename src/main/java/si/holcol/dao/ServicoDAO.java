package si.holcol.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Criacao de servicos e gestao dos seus membros (convites, adesoes, abandonos, eliminacao). */
public class ServicoDAO {

    /**
     * Cria o servico e a linha da subclasse (ROOM_COLAB, TEXT_CHAT ou TEXT_BLOG) na mesma transacao.
     * A regra "TEXT_BLOG so por pessoa singular" e garantida pelo trigger t_servico_blog_criador.
     */
    public int criar(Connection c, String tipo, String designacao, long nifCriador, LocalDate dataCriacao,
                     String modalidade, BigDecimal valorFixo, BigDecimal valorPorMembro) throws SQLException {
        String tabela = switch (tipo) {
            case "ROOM_COLAB" -> "room_colab";
            case "TEXT_CHAT"  -> "text_chat";
            case "TEXT_BLOG"  -> "text_blog";
            default -> throw new IllegalArgumentException("Tipo de servico invalido: " + tipo);
        };
        String sql = "INSERT INTO servico (tipo, designacao, nif_criador, data_criacao, modalidade, "
                + "valor_fixo_mensal, valor_por_membro) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        int id;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, designacao);
            ps.setLong(3, nifCriador);
            ps.setDate(4, Date.valueOf(dataCriacao));
            ps.setString(5, modalidade);
            ps.setBigDecimal(6, valorFixo);
            ps.setBigDecimal(7, valorPorMembro);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getInt(1);
            }
        }
        // o nome da tabela vem do switch acima (nao de input do utilizador)
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + tabela + " (id) VALUES (?)")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        return id;
    }

    public void convidar(Connection c, int idServico, long nifCliente, LocalDate dataConvite) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO adere (id_servico, nif_cliente, data_convite) VALUES (?, ?, ?)")) {
            ps.setInt(1, idServico);
            ps.setLong(2, nifCliente);
            ps.setDate(3, Date.valueOf(dataConvite));
            ps.executeUpdate();
        }
    }

    public void aderir(Connection c, int idServico, long nifCliente, LocalDate dataAdesao) throws SQLException {
        String sql = "UPDATE adere SET data_adesao = ? "
                + "WHERE id_servico = ? AND nif_cliente = ? AND data_adesao IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dataAdesao));
            ps.setInt(2, idServico);
            ps.setLong(3, nifCliente);
            exigirUmaLinha(ps.executeUpdate(), "Nao existe convite pendente para este cliente neste servico");
        }
    }

    public void abandonar(Connection c, int idServico, long nifCliente, LocalDate dataAbandono) throws SQLException {
        String sql = "UPDATE adere SET data_abandono = ? "
                + "WHERE id_servico = ? AND nif_cliente = ? AND data_adesao IS NOT NULL AND data_abandono IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dataAbandono));
            ps.setInt(2, idServico);
            ps.setLong(3, nifCliente);
            exigirUmaLinha(ps.executeUpdate(), "O cliente nao e membro ativo deste servico");
        }
    }

    /** Apenas o criador pode eliminar o servico. */
    public void eliminar(Connection c, int idServico, long nifCriador, LocalDate dataEliminacao) throws SQLException {
        String sql = "UPDATE servico SET data_eliminacao = ? "
                + "WHERE id = ? AND nif_criador = ? AND data_eliminacao IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dataEliminacao));
            ps.setInt(2, idServico);
            ps.setLong(3, nifCriador);
            exigirUmaLinha(ps.executeUpdate(),
                    "Servico inexistente, ja eliminado ou o NIF indicado nao e o do criador");
        }
    }

    public void listarMembros(Connection c, int idServico) throws SQLException {
        String sql = """
            SELECT s.nif_criador AS nif, 'CRIADOR' AS papel, s.data_criacao AS convite,
                   s.data_criacao AS adesao, NULL::date AS abandono
            FROM servico s WHERE s.id = ?
            UNION ALL
            SELECT a.nif_cliente, 'MEMBRO', a.data_convite, a.data_adesao, a.data_abandono
            FROM adere a WHERE a.id_servico = ?
            ORDER BY 2, 1
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idServico);
            ps.setInt(2, idServico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  %-9s NIF %d  convite=%s adesao=%s abandono=%s%n",
                            rs.getString("papel"), rs.getLong("nif"), rs.getDate("convite"),
                            rs.getDate("adesao"), rs.getDate("abandono"));
                }
            }
        }
    }

    private static void exigirUmaLinha(int linhas, String mensagem) {
        if (linhas != 1) {
            throw new IllegalArgumentException(mensagem);
        }
    }
}
