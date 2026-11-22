package si.holcol.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Registo de clientes (singulares e coletivos), moradas, contactos e metodos de pagamento. */
public class ClienteDAO {

    public record Morada(String rua, String numPorta, String andar, String letra,
                         String codigoPostal, String cidade, String pais) { }

    public record Contacto(String nome, String textChat) { }

    private static final int MAX_CONTACTOS = 5;

    // ---------- clientes ----------

    /** Regista cliente + pessoa singular. Deve ser chamado dentro de uma transacao. */
    public void registarSingular(Connection c, long nif, String email, Morada m, String nomeProprio,
                                 String apelido, String telemovel, LocalDate dataNascimento) throws SQLException {
        inserirCliente(c, nif, 'S', email, m);
        String sql = "INSERT INTO pessoa_singular (nif, nome_proprio, apelido, telemovel, data_nascimento) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            ps.setString(2, nomeProprio);
            ps.setString(3, apelido);
            ps.setString(4, telemovel);
            ps.setDate(5, Date.valueOf(dataNascimento));
            ps.executeUpdate();
        }
    }

    /** Regista cliente + pessoa coletiva + ate 5 contactos. Deve ser chamado dentro de uma transacao. */
    public void registarColetiva(Connection c, long nif, String email, Morada m, String designacao,
                                 List<Contacto> contactos) throws SQLException {
        if (contactos.size() > MAX_CONTACTOS) {
            throw new IllegalArgumentException("Uma pessoa coletiva tem no maximo " + MAX_CONTACTOS + " contactos");
        }
        inserirCliente(c, nif, 'C', email, m);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pessoa_coletiva (nif, designacao) VALUES (?, ?)")) {
            ps.setLong(1, nif);
            ps.setString(2, designacao);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO contacto (nif_coletiva, num_contacto, nome, text_chat) VALUES (?, ?, ?, ?)")) {
            int n = 1;
            for (Contacto ct : contactos) {
                ps.setLong(1, nif);
                ps.setInt(2, n++);
                ps.setString(3, ct.nome());
                ps.setString(4, ct.textChat());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void inserirCliente(Connection c, long nif, char tipo, String email, Morada m) throws SQLException {
        garantirCodigoPostal(c, m);
        String sql = "INSERT INTO cliente (nif, tipo, email, rua, num_porta, andar, letra, codigo_postal, pais) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            ps.setString(2, String.valueOf(tipo));
            ps.setString(3, email);
            ps.setString(4, m.rua());
            ps.setString(5, m.numPorta());
            ps.setString(6, m.andar());
            ps.setString(7, m.letra());
            ps.setString(8, m.codigoPostal());
            ps.setString(9, m.pais());
            ps.executeUpdate();
        }
    }

    /** codigo_postal -> cidade (3NF): cria a entrada se nao existir; rejeita cidade diferente da registada. */
    private void garantirCodigoPostal(Connection c, Morada m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT cidade FROM codigo_postal WHERE codigo_postal = ? AND pais = ?")) {
            ps.setString(1, m.codigoPostal());
            ps.setString(2, m.pais());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (!rs.getString(1).equalsIgnoreCase(m.cidade())) {
                        throw new IllegalArgumentException("O codigo postal " + m.codigoPostal()
                                + " ja esta registado para a cidade " + rs.getString(1));
                    }
                    return;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO codigo_postal (codigo_postal, pais, cidade) VALUES (?, ?, ?)")) {
            ps.setString(1, m.codigoPostal());
            ps.setString(2, m.pais());
            ps.setString(3, m.cidade());
            ps.executeUpdate();
        }
    }

    // ---------- metodos de pagamento ----------

    /** Regista um MBWay; devolve o id do metodo. O primeiro metodo de um cliente fica por omissao. */
    public int registarMBWay(Connection c, long nif, String telemovel, boolean porOmissao) throws SQLException {
        int id = inserirMetodo(c, nif, "MBWAY");
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO mbway (id, telemovel) VALUES (?, ?)")) {
            ps.setInt(1, id);
            ps.setString(2, telemovel);
            ps.executeUpdate();
        }
        escolherOmissao(c, nif, id, porOmissao);
        return id;
    }

    public int registarCartao(Connection c, long nif, String nomeCartao, String numero, YearMonth validade,
                              String codigo, boolean porOmissao) throws SQLException {
        int id = inserirMetodo(c, nif, "CARTAO");
        String sql = "INSERT INTO cartao_credito (id, nome_cartao, numero, data_validade, codigo) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, nomeCartao);
            ps.setString(3, numero);
            ps.setDate(4, Date.valueOf(validade.atEndOfMonth()));
            ps.setString(5, codigo);
            ps.executeUpdate();
        }
        escolherOmissao(c, nif, id, porOmissao);
        return id;
    }

    private int inserirMetodo(Connection c, long nif, String tipo) throws SQLException {
        String sql = "INSERT INTO metodo_pagamento (nif_cliente, tipo, por_omissao) VALUES (?, ?, FALSE) RETURNING id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void escolherOmissao(Connection c, long nif, int idMetodo, boolean pedido) throws SQLException {
        boolean primeiro;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM metodo_pagamento WHERE nif_cliente = ? AND id <> ?")) {
            ps.setLong(1, nif);
            ps.setInt(2, idMetodo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                primeiro = rs.getInt(1) == 0;
            }
        }
        if (pedido || primeiro) {
            definirPorOmissao(c, nif, idMetodo);
        }
    }

    /** Garante que apenas um metodo do cliente fica por omissao. Chamar dentro de uma transacao. */
    public void definirPorOmissao(Connection c, long nif, int idMetodo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE metodo_pagamento SET por_omissao = FALSE WHERE nif_cliente = ? AND por_omissao")) {
            ps.setLong(1, nif);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE metodo_pagamento SET por_omissao = TRUE WHERE id = ? AND nif_cliente = ?")) {
            ps.setInt(1, idMetodo);
            ps.setLong(2, nif);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("O metodo " + idMetodo + " nao pertence ao cliente " + nif);
            }
        }
    }

    public void listarMetodos(Connection c, long nif) throws SQLException {
        String sql = "SELECT id, tipo, por_omissao FROM metodo_pagamento WHERE nif_cliente = ? ORDER BY id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, nif);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  #%d  %-7s %s%n", rs.getInt("id"), rs.getString("tipo"),
                            rs.getBoolean("por_omissao") ? "(por omissao)" : "");
                }
            }
        }
    }
}
