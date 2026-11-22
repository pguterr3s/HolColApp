package si.holcol.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Pedidos de ajuda: registo, resposta por empregado e validacao pela chefia. */
public class AjudaDAO {

    public void registarPedido(Connection c, long nifCliente, LocalDateTime dataHora, String texto) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO mensagem_ajuda (nif_cliente, data_hora_pergunta, texto_pergunta) VALUES (?, ?, ?)")) {
            ps.setLong(1, nifCliente);
            ps.setTimestamp(2, Timestamp.valueOf(dataHora));
            ps.setString(3, texto);
            ps.executeUpdate();
        }
    }

    public void responder(Connection c, long nifCliente, LocalDateTime dataHoraPergunta, int numEmpregado,
                          String texto, LocalDateTime dataHoraResposta) throws SQLException {
        String sql = "UPDATE mensagem_ajuda SET texto_resposta = ?, data_hora_resposta = ?, num_empregado_resp = ? "
                + "WHERE nif_cliente = ? AND data_hora_pergunta = ? AND texto_resposta IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, texto);
            ps.setTimestamp(2, Timestamp.valueOf(dataHoraResposta));
            ps.setInt(3, numEmpregado);
            ps.setLong(4, nifCliente);
            ps.setTimestamp(5, Timestamp.valueOf(dataHoraPergunta));
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Pedido inexistente ou ja respondido");
            }
        }
    }

    /** A validacao so e possivel depois da resposta (garantido tambem pelo CHECK ck_msg_valida). */
    public void validar(Connection c, long nifCliente, LocalDateTime dataHoraPergunta, int numEmpregadoChefia)
            throws SQLException {
        String sql = "UPDATE mensagem_ajuda SET num_empregado_valida = ? "
                + "WHERE nif_cliente = ? AND data_hora_pergunta = ? "
                + "AND num_empregado_resp IS NOT NULL AND num_empregado_valida IS NULL";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, numEmpregadoChefia);
            ps.setLong(2, nifCliente);
            ps.setTimestamp(3, Timestamp.valueOf(dataHoraPergunta));
            if (ps.executeUpdate() != 1) {
                throw new IllegalArgumentException("Pedido inexistente, sem resposta ou ja validado");
            }
        }
    }

    public void listar(Connection c, long nifCliente) throws SQLException {
        String sql = "SELECT data_hora_pergunta, texto_pergunta, texto_resposta, data_hora_resposta, "
                + "num_empregado_resp, num_empregado_valida "
                + "FROM mensagem_ajuda WHERE nif_cliente = ? ORDER BY data_hora_pergunta";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nifCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  [%s] %s%n", rs.getTimestamp("data_hora_pergunta"), rs.getString("texto_pergunta"));
                    String resp = rs.getString("texto_resposta");
                    if (resp == null) {
                        System.out.println("     (sem resposta)");
                    } else {
                        Integer validador = (Integer) rs.getObject("num_empregado_valida");
                        System.out.printf("     resposta de empregado %d em %s: %s%s%n",
                                rs.getInt("num_empregado_resp"), rs.getTimestamp("data_hora_resposta"), resp,
                                validador == null ? " (nao validada)" : " (validada por " + validador + ")");
                    }
                }
            }
        }
    }
}
