package si.holcol.gui.panels;

import si.holcol.dao.AtividadeDAO;
import si.holcol.gui.util.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;

public class AtividadePanel extends JPanel {
    private final AtividadeDAO atividade = new AtividadeDAO();

    public AtividadePanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

        JButton btnSessao = new JButton("Iniciar Sessão");
        btnSessao.addActionListener(e -> {
            try {
                int room = Integer.parseInt(JOptionPane.showInputDialog(this, "ID da ROOM_COLAB:"));
                GuiUtils.transacao(c -> {
                    atividade.iniciarSessao(c, room, LocalDateTime.now());
                    System.out.println("✅ Sessão iniciada com sucesso.");
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Dados insuficientes.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnChat = new JButton("Publicar Chat");
        btnChat.addActionListener(e -> {
            try {
                int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do TEXT_CHAT:"));
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Autor:"));
                String txt = JOptionPane.showInputDialog(this, "Escreva a mensagem:");
                GuiUtils.transacao(c -> {
                    int num = atividade.publicarChat(c, id, nif, txt, LocalDateTime.now());
                    System.out.println("✅ Publicado no Chat com ID #" + num);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Autor sem acesso ou ID inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnBlog = new JButton("Publicar Blog");
        btnBlog.addActionListener(e -> {
            try {
                int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do TEXT_BLOG:"));
                long nif = Long.parseLong(JOptionPane.showInputDialog(this, "NIF do Autor:"));
                String tit = JOptionPane.showInputDialog(this, "Título do Post:");
                String txt = JOptionPane.showInputDialog(this, "Conteúdo do Post:");
                GuiUtils.transacao(c -> {
                    int num = atividade.publicarBlog(c, id, nif, tit, txt, LocalDateTime.now());
                    System.out.println("✅ Artigo de Blog publicado com ID #" + num);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Apenas pessoas singulares publicam em Blogs.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnListTextos = new JButton("Listar Textos");
        btnListTextos.addActionListener(e -> {
            try {
                int id = Integer.parseInt(JOptionPane.showInputDialog(this, "ID do Serviço:"));
                System.out.println("Textos publicados no Serviço #" + id + ":");
                GuiUtils.executar(c -> atividade.listarTextos(c, id));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "ID incorreto.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(btnSessao);
        add(btnChat);
        add(btnBlog);
        add(btnListTextos);
    }
}