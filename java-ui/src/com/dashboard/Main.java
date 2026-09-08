package com.dashboard;

import com.dashboard.view.DashboardFrame;
import javax.swing.*;

/**
 * Ponto de entrada da aplicação desktop.
 * <p>
 * Toda a construção e manipulação de componentes Swing deve acontecer na
 * Event Dispatch Thread (EDT); por isso usamos {@link SwingUtilities#invokeLater}.
 */
public class Main {

    public static void main(String[] args) {
        // Look and feel nativo do sistema operacional (opcional, deixa a
        // janela mais integrada ao SO; pode ser removido sem prejuízo).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Se falhar, seguimos com o look and feel padrão do Swing.
        }

        SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame();
            frame.setVisible(true);
        });
    }
}
