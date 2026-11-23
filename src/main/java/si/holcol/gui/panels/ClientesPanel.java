package si.holcol.gui.panels;

import si.holcol.dao.ClienteDAO;
import si.holcol.dao.ClienteDAO.Contacto;
import si.holcol.dao.ClienteDAO.Morada;
import si.holcol.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ClientesPanel extends JPanel {
    private final ClienteDAO clientes = new ClienteDAO();

    public ClientesPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JButton btnSingular = new JButton("Registar Pessoa Singular");
        btnSingular.addActionListener(e -> registarSingular());

        JButton btnColetiva = new JButton("Registar Pessoa Coletiva");
        btnColetiva.addActionListener(e -> registarColetiva());

        JButton btnMetodo = new JButton("Adicionar Método Pagamento");
        btnMetodo.addActionListener(e -> adicionarMetodo());

        JButton btnOmissao = new JButton("Definir Omissão");
        btnOmissao.addActionListener(e -> definirOmissao());

        JButton btnListar = new JButton("Listar Métodos");
        btnListar.addActionListener(e -> listarMetodos());

        add(btnSingular);
        add(btnColetiva);
        add(btnMetodo);
        add(btnOmissao);
        add(btnListar);
    }

    private void registarSingular() {
        JTextField txtNif = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtNome = new JTextField();
        JTextField txtApelido = new JTextField();
        JTextField txtTel = new JTextField();
        JTextField txtNasc = new JTextField(LocalDate.now().minusYears(18).toString());

        String[] labels = {"NIF:", "Email:", "Nome:", "Apelido:", "Telemóvel:", "Nascimento (yyyy-MM-dd):"};
        JComponent[] fields = {txtNif, txtEmail, txtNome, txtApelido, txtTel, txtNasc};

        if (JOptionPane.showConfirmDialog(this, GuiUtils.criarGridForm(labels, fields), "Novo Singular", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                long nif = Long.parseLong(txtNif.getText());
                LocalDate nasc = LocalDate.parse(txtNasc.getText());
                Morada m = GuiUtils.obterMoradaGUI(this);
                if (m != null) {
                    GuiUtils.transacao(c -> {
                        clientes.registarSingular(c, nif, txtEmail.getText(), m, txtNome.getText(), txtApelido.getText(), txtTel.getText(), nasc);
                        System.out.println("✅ Cliente Singular registado!");
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro de validação dos dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void registarColetiva() {
        JTextField txtNif = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtDesig = new JTextField();

        String[] labels = {"NIF:", "Email:", "Designação:"};
        JComponent[] fields = {txtNif, txtEmail, txtDesig};

        if (JOptionPane.showConfirmDialog(this, GuiUtils.criarGridForm(labels, fields), "Nova Empresa", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                long nif = Long.parseLong(txtNif.getText());
                Morada m = GuiUtils.obterMoradaGUI(this);
                if (m != null) {
                    List<Contacto> contactos = new ArrayList<>();
                    while (contactos.size() < 5 && JOptionPane.showConfirmDialog(this, "Adicionar contacto?", "Contactos", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        String nome = JOptionPane.showInputDialog(this, "Nome do Contacto:");
                        String chat = JOptionPane.showInputDialog(this, "TextChat:");
                        if (nome != null && chat != null) contactos.add(new Contacto(nome, chat));
                    }
                    GuiUtils.transacao(c -> {
                        clientes.registarColetiva(c, nif, txtEmail.getText(), m, txtDesig.getText(), contactos);
                        System.out.println("✅ Cliente Coletivo registado!");
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Dados ou NIF inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void adicionarMetodo() {
        try {
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
            String[] tipos = {"MBWAY", "CARTAO"};
            String tipo = (String) JOptionPane.showInputDialog(this, "Tipo de Método:", "Selecionar", JOptionPane.QUESTION_MESSAGE, null, tipos, tipos[0]);
            boolean omissao = JOptionPane.showConfirmDialog(this, "Definir por omissão?", "Omissão", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

            GuiUtils.transacao(c -> {
                if ("MBWAY".equals(tipo)) {
                    String tel = JOptionPane.showInputDialog(this, "Telemóvel MB Way:");
                    clientes.registarMBWay(c, nif, tel, omissao);
                } else {
                    String titular = JOptionPane.showInputDialog(this, "Nome no Cartão:");
                    String numero = JOptionPane.showInputDialog(this, "Número do Cartão:");
                    YearMonth val = YearMonth.parse(JOptionPane.showInputDialog(this, "Validade (yyyy-MM):"));
                    String codigo = JOptionPane.showInputDialog(this, "CVC:");
                    clientes.registarCartao(c, nif, titular, numero, val, codigo, omissao);
                }
                System.out.println("✅ Método de pagamento adicionado!");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Entrada inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void definirOmissao() {
        try {
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
            int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Método:"));
            GuiUtils.transacao(c -> {
                clientes.definirPorOmissao(c, nif, id);
                System.out.println("✅ Método por omissão atualizado!");
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro de processamento.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listarMetodos() {
        try {
            long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
            System.out.println("A listar métodos do cliente " + nif + "...");
            GuiUtils.executar(c -> clientes.listarMetodos(c, nif));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "NIF inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}