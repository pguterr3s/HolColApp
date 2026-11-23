package si.holcol.gui.panels;

import si.holcol.dao.AjudaDAO;
import si.holcol.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AjudaPanel extends JPanel {
    private final AjudaDAO ajuda = new AjudaDAO();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AjudaPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JButton btnPedido = new JButton("Pedir Suporte");
        btnPedido.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                String txt = JOptionPane.showInputDialog(this, "Descreva a sua dúvida:");
                GuiUtils.transacao(c -> {
                    ajuda.registarPedido(c, nif, LocalDateTime.now(), txt);
                    System.out.println("✅ Mensagem de suporte enviada para análise.");
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Entrada inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnResponder = new JButton("Responder");
        btnResponder.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                LocalDateTime perg = LocalDateTime.parse(JOptionPane.showInputDialog(this, "Data do Pedido (yyyy-MM-dd HH:mm):"), FORMATTER);
                int emp = Integer.parseInt(JOptionPane.showInputDialog(this, "Nº Empregado que responde:"));
                String res = JOptionPane.showInputDialog(this, "Texto da Resposta:");

                GuiUtils.transacao(c -> {
                    ajuda.responder(c, nif, perg, emp, res, LocalDateTime.now());
                    System.out.println("✅ Resposta registada.");
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Data em formato incorreto.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnValidar = new JButton("Validar (Chefia)");
        btnValidar.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                LocalDateTime perg = LocalDateTime.parse(JOptionPane.showInputDialog(this, "Data do Pedido (yyyy-MM-dd HH:mm):"), FORMATTER);
                int chefia = Integer.parseInt(JOptionPane.showInputDialog(this, "Nº ID da Chefia:"));

                GuiUtils.transacao(c -> {
                    ajuda.validar(c, nif, perg, chefia);
                    System.out.println("✅ Resposta de suporte validada pelo auditor.");
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Operação impedida (verifique se já foi respondido).", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnListar = new JButton("Consultar Pedidos");
        btnListar.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                System.out.println("Histórico de pedidos de suporte (NIF " + nif + "):");
                GuiUtils.executar(c -> ajuda.listar(c, nif));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "NIF incorreto.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(btnPedido);
        add(btnResponder);
        add(btnValidar);
        add(btnListar);
    }
}