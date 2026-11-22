package main.java.si.holcol.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Sessoes de colaboracao (ROOM_COLAB) e textos publicados (TEXT_CHAT / TEXT_BLOG). */
public class AtividadeDAO {

    // ---------- sessoes ----------

    public void iniciarSessao(Connection c, int idRoom, LocalDateTime inicio) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO sessao_colab (id_room, data_hora_inicio) VALUES (?, ?)")) {
            ps.setInt(1, idRoom);
            ps.setTimestamp(2, Timestamp.valueOf(inicio));
            ps.executeUpdate();
        }
    }

    public void encerrarSessao(Connection c, int idRoom, LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        // fecha a sessao e, na mesma transacao, a saida dos participantes que ainda la estavam
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE sessao_colab SET data_hora_fim = ? "
                        + "WHERE id_room = ? AND data_hora_inicio = ? AND data_hora_fim IS NULL")) {
            ps.setTimestamp(1, Timestamp.valueOf(fim));
            ps.setInt(2, idRoom);
            ps.setTimestamp(3, Timestamp.valueOf(inicio));
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Sessao inexistente ou ja encerrada");
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE participa SET data_hora_saida = ? "
                        + "WHERE id_room = ? AND data_hora_inicio = ? AND data_hora_saida IS NULL")) {
            ps.setTimestamp(1, Timestamp.valueOf(fim));
            ps.setInt(2, idRoom);
            ps.setTimestamp(3, Timestamp.valueOf(inicio));
            ps.executeUpdate();
        }
    }

    /** O trigger t_participa_membro garante que o cliente e membro do servico. */
    public void entrar(Connection c, int idRoom, LocalDateTime inicioSessao, long nif,
                       LocalDateTime entrada) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO participa (id_room, data_hora_inicio, nif_cliente, data_hora_entrada) "
                        + "VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, idRoom);
            ps.setTimestamp(2, Timestamp.valueOf(inicioSessao));
            ps.setLong(3, nif);
            ps.setTimestamp(4, Timestamp.valueOf(entrada));
            ps.executeUpdate();
        }
    }

    public void sair(Connection c, int idRoom, LocalDateTime inicioSessao, long nif,
                     LocalDateTime saida) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE participa SET data_hora_saida = ? "
                        + "WHERE id_room = ? AND data_hora_inicio = ? AND nif_cliente = ? AND data_hora_saida IS NULL")) {
            ps.setTimestamp(1, Timestamp.valueOf(saida));
            ps.setInt(2, idRoom);
            ps.setTimestamp(3, Timestamp.valueOf(inicioSessao));
            ps.setLong(4, nif);
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("O cliente nao esta presente nesta sessao");
            }
        }
    }

    public void listarSessoes(Connection c, int idRoom) throws SQLException {
        String sql = """
            SELECT s.data_hora_inicio, s.data_hora_fim, COUNT(p.nif_cliente) AS participantes
            FROM sessao_colab s
            LEFT JOIN participa p ON p.id_room = s.id_room AND p.data_hora_inicio = s.data_hora_inicio
            WHERE s.id_room = ?
            GROUP BY s.data_hora_inicio, s.data_hora_fim
            ORDER BY s.data_hora_inicio
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idRoom);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  inicio=%s fim=%s participantes=%d%n",
                            rs.getTimestamp(1), rs.getTimestamp(2), rs.getInt(3));
                }
            }
        }
    }

    // ---------- textos ----------

    /** num_texto fica NULL: o trigger t_tchat_num atribui o seguinte numero do servico. */
    public int publicarChat(Connection c, int idServico, long nifAutor, String conteudo,
                            LocalDateTime dataHora) throws SQLException {
        String sql = "INSERT INTO texto_chat (id_servico, num_texto, conteudo, data_hora_publicacao, nif_autor) "
                + "VALUES (?, NULL, ?, ?, ?) RETURNING num_texto";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idServico);
            ps.setString(2, conteudo);
            ps.setTimestamp(3, Timestamp.valueOf(dataHora));
            ps.setLong(4, nifAutor);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int publicarBlog(Connection c, int idServico, long nifAutor, String titulo, String conteudo,
                            LocalDateTime dataHora) throws SQLException {
        String sql = "INSERT INTO texto_blog (id_servico, num_texto, titulo, conteudo, data_hora_publicacao, nif_autor) "
                + "VALUES (?, NULL, ?, ?, ?, ?) RETURNING num_texto";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idServico);
            ps.setString(2, titulo);
            ps.setString(3, conteudo);
            ps.setTimestamp(4, Timestamp.valueOf(dataHora));
            ps.setLong(5, nifAutor);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void listarTextos(Connection c, int idServico) throws SQLException {
        String sql = """
            SELECT num_texto, NULL AS titulo, conteudo, data_hora_publicacao, nif_autor
            FROM texto_chat WHERE id_servico = ?
            UNION ALL
            SELECT num_texto, titulo, conteudo, data_hora_publicacao, nif_autor
            FROM texto_blog WHERE id_servico = ?
            ORDER BY 1
            """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idServico);
            ps.setInt(2, idServico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String titulo = rs.getString("titulo");
                    System.out.printf("  #%d [%s] NIF %d%s: %s%n", rs.getInt("num_texto"),
                            rs.getTimestamp("data_hora_publicacao"), rs.getLong("nif_autor"),
                            titulo == null ? "" : " \"" + titulo + "\"", rs.getString("conteudo"));
                }
            }
        }
    }
}
