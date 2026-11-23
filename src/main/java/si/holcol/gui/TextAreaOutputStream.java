package si.holcol.gui;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TextAreaOutputStream extends OutputStream {
    private final JTextArea textArea;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public TextAreaOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        buffer.write(b);
        if (b == '\n') {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        buffer.write(b, off, len);
        flushBuffer();
    }

    private void flushBuffer() {
        String text = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}