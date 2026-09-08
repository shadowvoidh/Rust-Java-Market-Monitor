package com.dashboard.view;

import com.dashboard.model.Quote;
import com.dashboard.model.QuotesResponse;
import com.dashboard.service.RustEngineClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janela principal do Dashboard de Cotações.
 */
public class DashboardFrame extends JFrame {

    private static final int AUTO_REFRESH_SECONDS = 30;

    // Paleta simples
    private static final Color BG_DARK = new Color(24, 26, 32);
    private static final Color PANEL_DARK = new Color(34, 37, 46);
    private static final Color GREEN = new Color(46, 204, 113);
    private static final Color RED = new Color(231, 76, 60);
    private static final Color TEXT_LIGHT = new Color(230, 230, 235);
    private static final Color TEXT_MUTED = new Color(150, 155, 165);
    private static final Color ACCENT = new Color(88, 101, 242);

    private final RustEngineClient client = new RustEngineClient();
    private final NumberFormat currencyFormat;

    // Componentes que precisam ser atualizados dinamicamente
    private final JPanel cardsPanel = new JPanel();
    private final Map<String, QuoteCardPanel> cardsBySymbol = new ConcurrentHashMap<>();
    private final DefaultTableModel tableModel;
    private final JLabel statusDot = new JLabel("●");
    private final JLabel statusLabel = new JLabel("Verificando...");
    private final JLabel lastSyncLabel = new JLabel("Última sincronização: --:--:--");
    private final JLabel countdownLabel = new JLabel();
    private final JButton refreshButton = new JButton("🔄  Atualizar Cotações");

    private Timer autoRefreshTimer;
    private Timer countdownTimer;
    private int secondsUntilNextRefresh = AUTO_REFRESH_SECONDS;

    public DashboardFrame() {
        super("Dashboard de Cotações — USD / EUR / BTC");

        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 640);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        cardsPanel.setLayout(new GridLayout(1, 3, 16, 16));
        cardsPanel.setOpaque(false);
        centerPanel.add(cardsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[]{"Símbolo", "Nome", "Preço (BRL)", "Variação %", "Última Atualização"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(PANEL_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 54, 64)));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Primeira carga + timers de atualização automática
        refreshQuotes();
        startAutoRefresh();
        startConnectionWatcher();
    }

    // =========================================================================
    // Construção da UI
    // =========================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL_DARK);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("💹  Cotações em Tempo Real");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(TEXT_LIGHT);
        header.add(title, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statusPanel.setOpaque(false);
        statusDot.setForeground(TEXT_MUTED);
        statusDot.setFont(new Font("SansSerif", Font.PLAIN, 16));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusPanel.add(statusDot);
        statusPanel.add(statusLabel);
        header.add(statusPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(PANEL_DARK);
        footer.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setOpaque(false);
        lastSyncLabel.setForeground(TEXT_MUTED);
        lastSyncLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countdownLabel.setForeground(TEXT_MUTED);
        countdownLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoPanel.add(lastSyncLabel);
        infoPanel.add(countdownLabel);
        footer.add(infoPanel, BorderLayout.WEST);

        refreshButton.setFocusPainted(false);
        refreshButton.setBackground(ACCENT);
        refreshButton.setForeground(Color.BLACK); // Texto do botão alterado para Preto
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        refreshButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        refreshButton.addActionListener(e -> refreshQuotes());
        footer.add(refreshButton, BorderLayout.EAST);

        return footer;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setBackground(PANEL_DARK);
        table.setForeground(TEXT_LIGHT);
        table.setGridColor(new Color(50, 54, 64));
        table.setSelectionBackground(ACCENT.darker());
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setBackground(new Color(44, 47, 58));
        table.getTableHeader().setForeground(Color.BLACK); // Texto dos títulos da tabela alterado para Preto
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.setFillsViewportHeight(true);
    }

    // =========================================================================
    // Timers
    // =========================================================================

    private void startAutoRefresh() {
        autoRefreshTimer = new Timer(AUTO_REFRESH_SECONDS * 1000, e -> refreshQuotes());
        autoRefreshTimer.start();

        countdownTimer = new Timer(1000, e -> {
            secondsUntilNextRefresh--;
            if (secondsUntilNextRefresh < 0) {
                secondsUntilNextRefresh = AUTO_REFRESH_SECONDS;
            }
            countdownLabel.setText("Próxima atualização automática em " + secondsUntilNextRefresh + "s");
        });
        countdownTimer.start();
    }

    private void startConnectionWatcher() {
        Timer watcher = new Timer(5000, e -> checkConnectionStatus());
        watcher.start();
        checkConnectionStatus();
    }

    private void checkConnectionStatus() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return client.isEngineOnline();
            }

            @Override
            protected void done() {
                try {
                    boolean online = get();
                    updateStatusIndicator(online);
                } catch (Exception ex) {
                    updateStatusIndicator(false);
                }
            }
        };
        worker.execute();
    }

    private void updateStatusIndicator(boolean online) {
        if (online) {
            statusDot.setForeground(GREEN);
            statusLabel.setText("Engine Online");
            statusLabel.setForeground(GREEN);
        } else {
            statusDot.setForeground(RED);
            statusLabel.setText("Engine Offline");
            statusLabel.setForeground(RED);
        }
    }

    // =========================================================================
    // Atualização de cotações
    // =========================================================================

    private void refreshQuotes() {
        secondsUntilNextRefresh = AUTO_REFRESH_SECONDS;
        refreshButton.setEnabled(false);
        refreshButton.setText("🔄  Atualizando...");

        SwingWorker<QuotesResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected QuotesResponse doInBackground() throws Exception {
                return client.fetchQuotes();
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                refreshButton.setText("🔄  Atualizar Cotações");
                try {
                    QuotesResponse response = get();
                    if (response != null && response.getQuotes() != null && !response.getQuotes().isEmpty()) {
                        renderQuotes(response);
                        updateStatusIndicator(true);
                    } else {
                        showEmptyState();
                    }
                } catch (Exception ex) {
                    updateStatusIndicator(false);
                    JOptionPane.showMessageDialog(
                            DashboardFrame.this,
                            "Não foi possível conectar à engine Rust em http://localhost:8080.\n" +
                                    "Verifique se o serviço está rodando (cargo run).",
                            "Engine indisponível",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void renderQuotes(QuotesResponse response) {
        cardsPanel.removeAll();
        List<Quote> quotes = response.getQuotes();
        cardsPanel.setLayout(new GridLayout(1, Math.max(quotes.size(), 1), 16, 16));

        for (Quote q : quotes) {
            QuoteCardPanel card = cardsBySymbol.computeIfAbsent(q.getSymbol(), s -> new QuoteCardPanel());
            card.updateData(q, currencyFormat);
            cardsPanel.add(card);
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();

        tableModel.setRowCount(0);
        for (Quote q : quotes) {
            tableModel.addRow(new Object[]{
                    q.getSymbol(),
                    q.getName(),
                    currencyFormat.format(q.getPrice()),
                    String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", q.getChangePercent()),
                    q.getLastUpdate()
            });
        }

        String cacheTag = response.isCached() ? " (cache)" : "";
        lastSyncLabel.setText("Última sincronização: " + response.getGeneratedAt() + cacheTag);
    }

    private void showEmptyState() {
        lastSyncLabel.setText("Nenhuma cotação disponível no momento.");
    }

    // =========================================================================
    // Card visual individual
    // =========================================================================

    private static class QuoteCardPanel extends JPanel {
        private final JLabel symbolLabel = new JLabel();
        private final JLabel nameLabel = new JLabel();
        private final JLabel priceLabel = new JLabel();
        private final JLabel changeLabel = new JLabel();

        QuoteCardPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(PANEL_DARK);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(50, 54, 64)),
                    new EmptyBorder(14, 16, 14, 16)
            ));

            symbolLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
            symbolLabel.setForeground(TEXT_LIGHT);

            nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            nameLabel.setForeground(TEXT_MUTED);

            priceLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            priceLabel.setForeground(TEXT_LIGHT);
            priceLabel.setBorder(new EmptyBorder(8, 0, 4, 0));

            changeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

            add(symbolLabel);
            add(nameLabel);
            add(priceLabel);
            add(changeLabel);
        }

        void updateData(Quote q, NumberFormat currencyFormat) {
            symbolLabel.setText(q.getSymbol());
            nameLabel.setText(q.getName());
            priceLabel.setText(currencyFormat.format(q.getPrice()));

            boolean positive = q.isPositive();
            String arrow = positive ? "▲" : "▼";
            changeLabel.setText(arrow + " " + String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", q.getChangePercent()));
            changeLabel.setForeground(positive ? GREEN : RED);
        }
    }
}