import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class TradingPlatform extends JFrame {

    // ==========================================
    // OOP CORE DOMAIN MODELS
    // ==========================================
    static class Stock {
        String ticker;
        String name;
        double price;

        public Stock(String ticker, String name, double initialPrice) {
            this.ticker = ticker;
            this.name = name;
            this.price = initialPrice;
        }

        // Simulates realistic market micro-fluctuations
        public void fluctuate() {
            Random rand = new Random();
            double percentChange = (rand.nextDouble() * 4.0 - 2.0) / 100.0; // -2% to +2%
            this.price += this.price * percentChange;
            if (this.price < 1.0) this.price = 1.0; // Prevent penny-stock bankruptcy crash
        }
    }

    static class User {
        double cashBalance;
        HashMap<String, Integer> portfolio = new HashMap<>(); // Ticker -> Shares Owned

        public User(double startingCash) {
            this.cashBalance = startingCash;
        }
    }

    static class Transaction {
        String type;
        String ticker;
        int shares;
        double absolutePrice;

        public Transaction(String type, String ticker, int shares, double absolutePrice) {
            this.type = type;
            this.ticker = ticker;
            this.shares = shares;
            this.absolutePrice = absolutePrice;
        }
    }

    // ==========================================
    // PLATFORM ENGINE PROPERTIES
    // ==========================================
    private final ArrayList<Stock> marketStocks = new ArrayList<>();
    private final ArrayList<Transaction> ledger = new ArrayList<>();
    private final User currentUser;
    private final String DATA_FILE = "portfolio_data.txt";

    // UI Dashboard Elements
    private DefaultTableModel marketTableModel;
    private DefaultTableModel portfolioTableModel;
    private JTextArea loggerTextArea;
    private JLabel totalNetWorthLabel;
    private JLabel availableCashLabel;
    
    // Cyberpunk Color Matrix
    private final Color COLOR_BG = new Color(13, 13, 18);
    private final Color COLOR_CARD = new Color(22, 22, 30);
    private final Color COLOR_TEXT = new Color(240, 242, 245);
    private final Color COLOR_BULL = new Color(0, 230, 118);   // Neon Green
    private final Color COLOR_BEAR = new Color(255, 61, 0);    // Cyber Red
    private final Color COLOR_MUTED = new Color(110, 115, 130);

    public TradingPlatform() {
        // Initialize Mock Market Stocks
        marketStocks.add(new Stock("AMZN", "Amazon.com Inc.", 175.50));
        marketStocks.add(new Stock("AAPL", "Apple Inc.", 180.20));
        marketStocks.add(new Stock("TSLA", "Tesla Motors", 195.80));
        marketStocks.add(new Stock("NVDA", "NVIDIA Corp.", 875.00));
        marketStocks.add(new Stock("MSFT", "Microsoft Corp.", 420.10));

        // Attempt Persistence File I/O Recovery, fallback to default user profile
        currentUser = loadPortfolioData();

        // Frame Configuration
        setTitle("QUANTUM TERMINAL // CORE MARKET ENGINE");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(15, 15));

        // --- TOP PANEL: Live Portfolio Balance Ribbon ---
        JPanel headerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        headerPanel.setBackground(COLOR_CARD);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 2, 0, COLOR_BULL),
                BorderFactory.createEmptyBorder(15, 25, 15, 25)));

        totalNetWorthLabel = new JLabel("PORTFOLIO VALUATION: $0.00", SwingConstants.LEFT);
        totalNetWorthLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        totalNetWorthLabel.setForeground(COLOR_BULL);

        availableCashLabel = new JLabel("LIQUID CASH: $0.00", SwingConstants.RIGHT);
        availableCashLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        availableCashLabel.setForeground(COLOR_TEXT);

        headerPanel.add(totalNetWorthLabel);
        headerPanel.add(availableCashLabel);
        add(headerPanel, BorderLayout.NORTH);

        // --- MAIN DASHBOARD BODY GRID ---
        JPanel mainBodyPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        mainBodyPanel.setBackground(COLOR_BG);
        mainBodyPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        // LEFT COLUMN: Live Market Feeds & Asset Execution
        JPanel leftColumnPanel = new JPanel(new BorderLayout(0, 15));
        leftColumnPanel.setBackground(COLOR_BG);
        
        marketTableModel = new DefaultTableModel(new String[]{"TICKER", "ASSET NAME", "LIVE PRICE"}, 0);
        JTable marketTable = createStyledTable(marketTableModel);
        JScrollPane marketScroll = new JScrollPane(marketTable);
        marketScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_MUTED), "LIVE LIQUIDITY MARKETS", 0, 0, null, COLOR_BULL));
        marketScroll.getViewport().setBackground(COLOR_CARD);
        leftColumnPanel.add(marketScroll, BorderLayout.CENTER);

        // Order Execution Module Panel
        JPanel executionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        executionPanel.setBackground(COLOR_CARD);
        executionPanel.setBorder(BorderFactory.createLineBorder(new Color(45, 45, 60)));

        JTextField tickerInput = createStyledField(6);
        JTextField sharesInput = createStyledField(4);
        JButton buyButton = createStyledButton("EXECUTE BUY ORDER", COLOR_BULL);
        JButton sellButton = createStyledButton("LIQUIDATE ASSET", COLOR_BEAR);

        executionPanel.add(new JLabel("TICKER:") {{ setForeground(COLOR_MUTED); }});
        executionPanel.add(tickerInput);
        executionPanel.add(new JLabel("SHARES:") {{ setForeground(COLOR_MUTED); }});
        executionPanel.add(sharesInput);
        executionPanel.add(buyButton);
        executionPanel.add(sellButton);
        leftColumnPanel.add(executionPanel, BorderLayout.SOUTH);

        // RIGHT COLUMN: Personal Portfolio Tracker & Transaction Logging Audits
        JPanel rightColumnPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        rightColumnPanel.setBackground(COLOR_BG);

        portfolioTableModel = new DefaultTableModel(new String[]{"HELD ASSET", "SHARES OWNED", "CURRENT VALUE"}, 0);
        JTable portfolioTable = createStyledTable(portfolioTableModel);
        JScrollPane portfolioScroll = new JScrollPane(portfolioTable);
        portfolioScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_MUTED), "SECURED PORTFOLIO POSITIONS", 0, 0, null, COLOR_TEXT));
        portfolioScroll.getViewport().setBackground(COLOR_CARD);
        rightColumnPanel.add(portfolioScroll);

        loggerTextArea = new JTextArea();
        loggerTextArea.setBackground(new Color(10, 10, 14));
        loggerTextArea.setForeground(new Color(150, 255, 150));
        loggerTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        loggerTextArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(loggerTextArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_MUTED), "SYSTEM LEDGER AUDIT LOGS", 0, 0, null, COLOR_MUTED));
        rightColumnPanel.add(logScroll);

        mainBodyPanel.add(leftColumnPanel);
        mainBodyPanel.add(rightColumnPanel);
        add(mainBodyPanel, BorderLayout.CENTER);

        // Populate Initial Render Views
        updateMarketDataGrid();
        recalculateValuationMetrics();

        // --- BACKGROUND LIVE MARKET PIPELINE THREAD (CONCURRENCY FLEX) ---
        Thread marketTickerEngine = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(2000); // Tickers fluctuate dynamically every 2 seconds
                    for (Stock s : marketStocks) {
                        s.fluctuate();
                    }
                    // Swing thread safe UI updates
                    SwingUtilities.invokeLater(() -> {
                        updateMarketDataGrid();
                        recalculateValuationMetrics();
                    });
                }
            } catch (InterruptedException e) {
                logMessage("[SYSTEM SYSTEM EXCEPTION] Live Market Stream pipeline disrupted.");
            }
        });
        marketTickerEngine.setDaemon(true); // Shuts down smoothly when app closes
        marketTickerEngine.start();

        // --- ORDER PROCESSING LOGIC ---
        buyButton.addActionListener(e -> handleOrder(tickerInput.getText().toUpperCase().trim(), sharesInput.getText().trim(), true));
        sellButton.addActionListener(e -> handleOrder(tickerInput.getText().toUpperCase().trim(), sharesInput.getText().trim(), false));

        // Gracefully persist state data upon frame termination
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                savePortfolioData();
            }
        });

        logMessage("[SYSTEM ONLINE] Quantum Trading Engine deployed successfully.");
    }

    // ==========================================
    // DATA LAYER & UI HELPER ROUTINES
    // ==========================================
    private void updateMarketDataGrid() {
        marketTableModel.getRowCount();
        marketTableModel.setRowCount(0);
        for (Stock s : marketStocks) {
            marketTableModel.addRow(new Object[]{s.ticker, s.name, String.format("$%.2f", s.price)});
        }
    }

    private void recalculateValuationMetrics() {
        double portfolioValue = 0;
        portfolioTableModel.setRowCount(0);

        for (String ticker : currentUser.portfolio.keySet()) {
            int shares = currentUser.portfolio.get(ticker);
            if (shares > 0) {
                Stock targetStock = findMarketStock(ticker);
                double assetWorth = (targetStock != null) ? targetStock.price * shares : 0;
                portfolioValue += assetWorth;

                portfolioTableModel.addRow(new Object[]{ticker, shares, String.format("$%.2f", assetWorth)});
            }
        }

        double totalNetWorth = currentUser.cashBalance + portfolioValue;
        totalNetWorthLabel.setText(String.format("PORTFOLIO VALUATION: $%.2f", totalNetWorth));
        availableCashLabel.setText(String.format("LIQUID CASH: $%.2f", currentUser.cashBalance));
    }

    private void handleOrder(String ticker, String shareStr, boolean isBuyOrder) {
        Stock stock = findMarketStock(ticker);
        if (stock == null) {
            JOptionPane.showMessageDialog(this, "Target Asset Ticker outside system inventory scope.", "Trade Refused", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int shares = Integer.parseInt(shareStr);
            if (shares <= 0) throw new NumberFormatException();

            double orderCost = stock.price * shares;

            if (isBuyOrder) {
                if (currentUser.cashBalance >= orderCost) {
                    currentUser.cashBalance -= orderCost;
                    currentUser.portfolio.put(ticker, currentUser.portfolio.getOrDefault(ticker, 0) + shares);
                    ledger.add(new Transaction("BUY", ticker, shares, stock.price));
                    logMessage(String.format("[TRADE ORDER EXECUTION] Bought %d shares of %s at $%.2f", shares, ticker, stock.price));
                } else {
                    JOptionPane.showMessageDialog(this, "Insufficient liquid capital reserves.", "Trade Cleared Denied", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                int ownedShares = currentUser.portfolio.getOrDefault(ticker, 0);
                if (ownedShares >= shares) {
                    currentUser.cashBalance += orderCost;
                    int remainingShares = ownedShares - shares;

if (remainingShares == 0) {
    currentUser.portfolio.remove(ticker);
} else {
    currentUser.portfolio.put(ticker, remainingShares);
}
                    ledger.add(new Transaction("SELL", ticker, shares, stock.price));
                    logMessage(String.format("[ASSET LIQUIDATION] Sold %d shares of %s at $%.2f", shares, ticker, stock.price));
                } else {
                    JOptionPane.showMessageDialog(this, "Position holdings insufficient to meet requested liquidation volume.", "Trade Cleared Denied", JOptionPane.WARNING_MESSAGE);
                }
            }

            recalculateValuationMetrics();
            savePortfolioData(); // Save instantly on execution

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Transactional payload quantity must be a positive whole integer.", "Syntax Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Stock findMarketStock(String ticker) {
        for (Stock s : marketStocks) {
            if (s.ticker.equalsIgnoreCase(ticker)) return s;
        }
        return null;
    }

    private void logMessage(String rawMessage) {
        loggerTextArea.append(" " + rawMessage + "\n");
    }

    // ==========================================
    // PERSISTENCE ENGINE (FILE I/O CORES)
    // ==========================================
    private void savePortfolioData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            writer.println(currentUser.cashBalance);
            for (String ticker : currentUser.portfolio.keySet()) {
                writer.println(ticker + "," + currentUser.portfolio.get(ticker));
            }
        } catch (IOException e) {
            System.err.println("Critical Error executing File I/O save state protocol.");
        }
    }

    private User loadPortfolioData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return new User(50000.00); // Default seed account with $50k capital

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            double cash = Double.parseDouble(reader.readLine());
            User loadedUser = new User(cash);
            String positionLine;
            while ((positionLine = reader.readLine()) != null) {
                String[] tokens = positionLine.split(",");
                if (tokens.length == 2) {
                    loadedUser.portfolio.put(tokens[0], Integer.parseInt(tokens[1]));
                }
            }
            return loadedUser;
        } catch (Exception e) {
            return new User(50000.00); // Redundant fail-safe profile reset
        }
    }

    // ==========================================
    // STYLING COMPONENT CONSTRUCTORS
    // ==========================================
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(COLOR_CARD);
        table.setForeground(COLOR_TEXT);
        table.setGridColor(new Color(38, 38, 50));
        table.setRowHeight(28);
        table.setFont(new Font("Monospaced", Font.PLAIN, 13));
        table.setFillsViewportHeight(true);

        JTableHeader h = table.getTableHeader();
        h.setBackground(COLOR_BG);
        h.setForeground(COLOR_MUTED);
        h.setFont(new Font("Monospaced", Font.BOLD, 12));

        DefaultTableCellRenderer cRenderer = new DefaultTableCellRenderer();
        cRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, cRenderer);

        return table;
    }

    private JTextField createStyledField(int width) {
        JTextField f = new JTextField(width);
        f.setBackground(COLOR_BG);
        f.setForeground(COLOR_TEXT);
        f.setCaretColor(COLOR_BULL);
        f.setFont(new Font("Monospaced", Font.BOLD, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 80)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return f;
    }

    private JButton createStyledButton(String label, Color highlightColor) {
        JButton b = new JButton(label);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setBackground(highlightColor);
        b.setForeground(COLOR_BG);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return b;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TradingPlatform().setVisible(true));
    }
}