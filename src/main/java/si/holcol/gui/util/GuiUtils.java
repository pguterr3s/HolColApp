package si.holcol.gui.util;

import si.holcol.dao.ClienteDAO.Morada;
import si.holcol.db.ConnectionFactory;
import si.holcol.db.Tx;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class GuiUtils {

    @FunctionalInterface
    public interface Acao {
        void executar(Connection c) throws SQLException;
    }

    public static void executar(Acao acao) {
        try (Connection c = ConnectionFactory.getConnection()) {
            acao.executar(c);
        } catch (SQLException e) {
            System.out.println("❌ Erro de base de dados: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("⚠️ Operação recusada: " + e.getMessage());
        }
    }

    public static void transacao(Acao acao) {
        executar(c -> Tx.run(c, conn -> { acao.executar(conn); return null; }));
    }

    public static JPanel criarGridForm(String[] labels, JComponent[] fields) {
        JPanel formPanel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < labels.length; i++) {
            formPanel.add(new JLabel(labels[i], SwingConstants.RIGHT));
            formPanel.add(fields[i]);
        }
        return formPanel;
    }

    public static Morada obterMoradaGUI(Component parent) {
        JTextField txtRua = new JTextField();
        JTextField txtPorta = new JTextField();
        JTextField txtAndar = new JTextField();
        JTextField txtLetra = new JTextField();
        JTextField txtCp = new JTextField();
        JTextField txtCidade = new JTextField();
        JTextField txtPais = new JTextField("Portugal");

        String[] labels = {"Rua:", "Nº Porta:", "Andar:", "Letra:", "Código Postal:", "Cidade:", "País:"};
        JComponent[] fields = {txtRua, txtPorta, txtAndar, txtLetra, txtCp, txtCidade, txtPais};

        int option = JOptionPane.showConfirmDialog(parent, criarGridForm(labels, fields), "Morada do Cliente", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            return new Morada(txtRua.getText().trim(), txtPorta.getText().trim(),
                    txtAndar.getText().trim().isEmpty() ? null : txtAndar.getText().trim(),
                    txtLetra.getText().trim().isEmpty() ? null : txtLetra.getText().trim(),
                    txtCp.getText().trim(), txtCidade.getText().trim(), txtPais.getText().trim());
        }
        return null;
    }
}