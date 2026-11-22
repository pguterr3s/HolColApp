package si.holcol.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

/** Faturacao mensal (modelo Free/Premium) e registo do resultado do pagamento automatico. */
public class FaturacaoDAO {

    /** Resultado do processamento mensal de um cliente. */
    public record Resultado(boolean emitida, int numeroFatura, BigDecimal montante, String motivo) { }

    /**
     * Custo do mes para um cliente: soma dos custos dos servicos que criou e que estiveram ativos no mes.
     * Free: ate 3 membros aderentes ativos (fim do mes) custo 0. Acima disso: valor fixo mensal ou
     * valor por membro a partir do 4.o. Os membros contados sao os que aderiram e nao abandonaram.
     */
    public BigDecimal calcularMontante(Connection c, long nif, YearMonth mes) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN n.membros <= 3        THEN 0
                                     WHEN s.modalidade = 'FIXO' THEN s.valor_fixo_mensal
                                     ELSE s.valor_por_membro * (n.membros - 3) END), 0)
            FROM servico s
            CROSS JOIN LATERAL (
                SELECT COUNT(*) AS membros
                FROM adere a
                WHERE a.id_servico = s.id
                  AND a.data_adesao IS NOT NULL AND a.data_adesao <= ?
                  AND (a.data_abandono IS NULL OR a.data_abandono > ?)) n
            WHERE s.nif_criador = ?
              AND s.data_criacao <= ?
              AND (s.data_eliminacao IS NULL OR s.data_eliminacao >= ?)
            """;
        Date fim = Date.valueOf(mes.atEndOfMonth());
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, fim);
            ps.setDate(2, fim);
            ps.setLong(3, nif);
            ps.setDate(4, fim);
            ps.setDate(5, Date.valueOf(mes.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1).setScale(2, RoundingMode.HALF_UP);
            }
        }
    }

    /**
     * Emite a fatura do mes (estado PENDENTE) com o metodo de pagamento por omissao.
     * Empregados estao isentos e montantes nulos (modelo Free) nao geram fatura.
     * Deve ser chamado dentro de uma transacao.
     */
    public Resultado emitirFatura(Connection c, long nif, YearMonth mes) throws SQLException {
        if (existe(c, "SELECT 1 FROM empregado WHERE nif = ?", nif)) {
            return new Resultado(false, 0, BigDecimal.ZERO, "Empregado isento de pagamento");
        }
        if (!existe(c, "SELECT 1 FROM cliente WHERE nif = ?", nif)) {
            throw new IllegalArgumentException("Cliente inexistente: " + nif);
        }
        BigDecimal montante = calcularMontante(c, nif, mes);
        if (montante.signum() == 0) {
            return new Resultado(false, 0, montante, "Montante 0,00 (modelo Free): nao ha fatura a emitir");
        }
        Integer metodo = metodoPorOmissao(c, nif);
        if (metodo == null) {
            throw new IllegalArgumentException("O cliente nao tem metodo de pagamento por omissao");
        }
        String sql = "INSERT INTO fatura (nif_cliente, id_metodo, mes_referencia, montante) "
                + "VALUES (?, ?, ?, ?) RETURNING numero";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            ps.setInt(2, metodo);
            ps.setDate(3, Date.valueOf(mes.atDay(1)));
            ps.setBigDecimal(4, montante);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Resultado(true, rs.getInt(1), montante, "Fatura emitida (PENDENTE)");
            } catch (SQLException e) {
                if ("23505".equals(e.getSQLState())) {
                    throw new IllegalArgumentException("Ja existe fatura deste cliente para " + mes);
                }
                throw e;
            }
        }
    }

    /**
     * Simula a tentativa de pagamento automatico: MBWay e aceite; cartao so se ainda estiver
     * dentro da validade. Regista o resultado (PAGO / FALHADO). Devolve true se o pagamento foi aceite.
     */
    public boolean tentarPagamento(Connection c, int numeroFatura) throws SQLException {
        String sql = """
            SELECT f.estado_pagamento, mp.tipo, cc.data_validade
            FROM fatura f
            JOIN metodo_pagamento mp ON mp.id = f.id_metodo
            LEFT JOIN cartao_credito cc ON cc.id = mp.id
            WHERE f.numero = ?
            FOR UPDATE OF f
            """;
        boolean aceite;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, numeroFatura);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Fatura inexistente: " + numeroFatura);
                }
                if ("PAGO".equals(rs.getString("estado_pagamento"))) {
                    throw new IllegalArgumentException("A fatura " + numeroFatura + " ja esta paga");
                }
                Date validade = rs.getDate("data_validade");
                aceite = "MBWAY".equals(rs.getString("tipo"))
                        || (validade != null && !validade.toLocalDate().isBefore(LocalDate.now()));
            }
        }
        String upd = aceite
                ? "UPDATE fatura SET estado_pagamento = 'PAGO', data_pagamento = CURRENT_TIMESTAMP WHERE numero = ?"
                : "UPDATE fatura SET estado_pagamento = 'FALHADO', data_pagamento = NULL WHERE numero = ?";
        try (PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setInt(1, numeroFatura);
            ps.executeUpdate();
        }
        return aceite;
    }

    public void listarFaturas(Connection c, long nif) throws SQLException {
        String sql = "SELECT numero, mes_referencia, data_emissao, montante, estado_pagamento, id_metodo "
                + "FROM fatura WHERE nif_cliente = ? ORDER BY mes_referencia DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  fatura %d  mes=%s emitida=%s montante=%s estado=%s metodo=#%d%n",
                            rs.getInt("numero"), rs.getDate("mes_referencia"), rs.getDate("data_emissao"),
                            rs.getBigDecimal("montante"), rs.getString("estado_pagamento"), rs.getInt("id_metodo"));
                }
            }
        }
    }

    private Integer metodoPorOmissao(Connection c, long nif) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM metodo_pagamento WHERE nif_cliente = ? AND por_omissao")) {
            ps.setLong(1, nif);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private boolean existe(Connection c, String sql, long nif) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
