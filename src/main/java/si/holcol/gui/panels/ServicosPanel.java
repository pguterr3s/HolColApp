package si.holcol.gui.panels;

import si.holcol.dao.ServicoDAO;
import si.holcol.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ServicosPanel extends JPanel {
    private final ServicoDAO servicos = new ServicoDAO();

    public ServicosPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JButton btnCriar = new JButton("Criar Serviço");
        btnCriar.addActionListener(e -> criarServico());

        JButton btnConvite = new JButton("Convidar Cliente");
        btnConvite.addActionListener(e -> convidar());

        JButton btnAderir = new JButton("Registar Adesão");
        btnAderir.addActionListener(e -> aderir());

        JButton btnAbandono = new JButton("Registar Abandono");
        btnAbandono.addActionListener(e -> abandonar());

        JButton btnEliminar = new JButton("Eliminar Serviço");
        btnEliminar.addActionListener(e -> eliminar());

        JButton btnListarMembros = new JButton("Listar Membros");
        btnListarMembros.addActionListener(e -> listarMembros());

        add(btnCriar);
        add(btnConvite);
        add(btnAderir);
        add(btnAbandono);
        add(btnEliminar);
        add(btnListarMembros);
    }

    private void criarServico() {
        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"ROOM_COLAB", "TEXT_CHAT", "TEXT_BLOG"});
        JTextField txtDes = new JTextField();
        JTextField txtCriador = new JTextField();
        JTextField txtData = new JTextField(LocalDate.now().toString());
        JComboBox<String> cbMod = new JComboBox<>(new String[]{"FIXO", "VARIAVEL"});
        JTextField txtFixo = new JTextField("0.00");
        JTextField txtMembro = new JTextField("0.00");

        String[] labels = {"Tipo:", "Designação:", "NIF Criador:", "Criação:", "Modalidade:", "Fixo:", "Por Membro:"};
        JComponent[] fields = {cbTipo, txtDes, txtCriador, txtData, cbMod, txtFixo, txtMembro};

        if (JOptionPane.showConfirmDialog(this, GuiUtils.criarGridForm(labels, fields), "Novo Serviço", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                long criador = Long.parseLong(txtCriador.getText());
                LocalDate date = LocalDate.parse(txtData.getText());
                BigDecimal fixo = new BigDecimal(txtFixo.getText());
                BigDecimal membro = new BigDecimal(txtMembro.getText());

                GuiUtils.transacao(c -> {
                    int id = servicos.criar(c, (String) cbTipo.getSelectedItem(), txtDes.getText(), criador, date, (String) cbMod.getSelectedItem(), fixo, membro);
                    System.out.println("✅ Serviço criado! ID: " + id);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro de digitação numérica ou datas.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void convidar() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Serviço:"));
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF a Convidar:"));
            GuiUtils.transacao(c -> {
                servicos.convidar(c, id, nif, LocalDate.now());
                System.out.println("✅ Convite registado!");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Inputs incorretos.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aderir() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Serviço:"));
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
            GuiUtils.transacao(c -> {
                servicos.aderir(c, id, nif, LocalDate.now());
                System.out.println("✅ Adesão consumada!");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao aderir.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abandonar() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Serviço:"));
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
            GuiUtils.transacao(c -> {
                servicos.abandonar(c, id, nif, LocalDate.now());
                System.out.println("✅ Abandono registado!");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao registar saída.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Serviço:"));
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Criador:"));
            GuiUtils.transacao(c -> {
                servicos.eliminar(c, id, nif, LocalDate.now());
                System.out.println("✅ Serviço eliminado logicamente.");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Apenas o criador pode remover.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listarMembros() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Serviço:"));
            System.out.println("Membros associados ao Serviço #" + id + ":");
            GuiUtils.executar(c -> servicos.listarMembros(c, id));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "ID inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}