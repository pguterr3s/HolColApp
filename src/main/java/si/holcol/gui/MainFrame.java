package si.holcol.gui;

import si.holcol.gui.panels.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("EUColab (HolColApp) - Gestão Integrada");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // Gestor de Abas com Paineis Modulares
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.addTab("👤 Clientes", new ClientesPanel());
        tabbedPane.addTab("⚙️ Serviços", new ServicosPanel());
        tabbedPane.addTab("📈 Atividade", new AtividadePanel());
        tabbedPane.addTab("💳 Faturação", new FaturacaoPanel());
        tabbedPane.addTab("❓ Pedidos de Ajuda", new AjudaPanel());

        // Consola Estilizada na Base
        JTextArea txtConsole = new JTextArea();
        txtConsole.setEditable(false);
        txtConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsole.setBackground(new Color(25, 25, 25));
        txtConsole.setForeground(new Color(57, 255, 20));
        txtConsole.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scrollConsole = new JScrollPane(txtConsole);
        scrollConsole.setPreferredSize(new Dimension(1000, 230));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Consola Atividade SQL"
        );
        border.setTitleColor(Color.DARK_GRAY);
        scrollConsole.setBorder(border);

        // Desvia streams do stdout para o componente gráfico de forma limpa
        PrintStream printStream = new PrintStream(new TextAreaOutputStream(txtConsole), true, StandardCharsets.UTF_8);
        System.setOut(printStream);
        System.setErr(printStream);

        add(tabbedPane, BorderLayout.CENTER);
        add(scrollConsole, BorderLayout.SOUTH);
    }
}