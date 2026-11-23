package si.holcol.gui.panels;

import si.holcol.dao.FaturacaoDAO;
import si.holcol.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;

public class FaturacaoPanel extends JPanel {
    private final FaturacaoDAO faturacao = new FaturacaoDAO();

    public FaturacaoPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JButton btnCalcular = new JButton("Apurar Custo Mensal");
        btnCalcular.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                YearMonth mes = YearMonth.parse(JOptionPane.showInputDialog(this, "Período (yyyy-MM):"));
                GuiUtils.executar(c -> {
                    BigDecimal valor = faturacao.calcularMontante(c, nif, mes);
                    System.out.printf("💰 Custos para %s do NIF %d: %s €%n", mes, nif, valor);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Dados mal formatados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnEmitir = new JButton("Emitir Fatura");
        btnEmitir.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                YearMonth mes = YearMonth.parse(JOptionPane.showInputDialog(this, "Período (yyyy-MM):"));
                GuiUtils.transacao(c -> {
                    FaturacaoDAO.Resultado r = faturacao.emitirFatura(c, nif, mes);
                    System.out.println("📢 " + r.motivo() + (r.emitida() ? " | Fatura: " + r.numeroFatura() + " | Total: " + r.montante() + " €" : ""));
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Incapaz de faturar (ex: Empregado isento).", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton btnPagar = new JButton("Simular Pagamento");
        btnPagar.addActionListener(e -> {
            try {
                int id = Integer.parseInt(JOptionPane.showInputDialog(this, "Nº Fatura:"));
                GuiUtils.transacao(c -> {
                    boolean pago = faturacao.tentarPagamento(c, id);
                    System.out.println(pago ? "✅ Pagamento aprovado com sucesso!" : "❌ Transação recusada/Cartão expirado.");
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Fatura desconhecida.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnListar = new JButton("Histórico de Faturas");
        btnListar.addActionListener(e -> {
            try {
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Cliente:"));
                System.out.println("Faturas associadas ao cliente " + nif + ":");
                GuiUtils.executar(c -> faturacao.listarFaturas(c, nif));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro de pesquisa.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(btnCalcular);
        add(btnEmitir);
        add(btnPagar);
        add(btnListar);
    }
}