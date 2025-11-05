package tudien;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

public class Server {
    private static final int PORT = 2000;
    private static Map<String, WordEntry> enVi = new HashMap<>();
    private static Map<String, WordEntry> viEn = new HashMap<>();

    // JDBC config
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dictionarydb?useUnicode=true&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "MySql@123";

    // GUI
    private static JFrame frame;
    private static JTextArea logArea, statusArea;
    private static JTable clientTable;
    private static DefaultTableModel clientTableModel;
    private static JButton startButton, stopButton, disconnectButton;
    private static ServerSocket serverSocket;
    private static Thread serverThread;
    private static volatile boolean running = false;

    private static List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    static class WordEntry {
        String meaning, ipa, partOfSpeech, exampleEn, exampleVi, imagePath;

        WordEntry(String meaning, String ipa, String partOfSpeech,
                  String exampleEn, String exampleVi, String imagePath) {
            this.meaning = meaning;
            this.ipa = ipa;
            this.partOfSpeech = partOfSpeech;
            this.exampleEn = exampleEn;
            this.exampleVi = exampleVi;
            this.imagePath = imagePath;
        }

        @Override
        public String toString() {
            return (meaning == null ? "" : meaning) + "|" +
                    (ipa == null ? "" : ipa) + "|" +
                    (partOfSpeech == null ? "" : partOfSpeech) + "|" +
                    (exampleEn == null ? "" : exampleEn) + "|" +
                    (exampleVi == null ? "" : exampleVi) + "|" +
                    (imagePath == null ? "" : imagePath);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Server::initGUI);
    }

    private static void initGUI() {
        frame = new JFrame("Dictionary Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        JButton editDictButton = new JButton("Chỉnh sửa Từ điển");
        JButton reloadButton = new JButton("Load lại dữ liệu");

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        editDictButton.addActionListener(e -> openDictionaryEditor());
        reloadButton.addActionListener(e -> {
            loadDictionaryFromDatabase();
            appendStatus("Đã tải lại dữ liệu từ điển từ database.");
        });

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(editDictButton);
        controlPanel.add(reloadButton);
        frame.add(controlPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        statusArea = new JTextArea();
        statusArea.setEditable(false);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Dictionary Log"));

        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder("Server Status"));
        statusScroll.setPreferredSize(new Dimension(900, 150));

        String[] columns = {"Client IP", "Port", "Status"};
        clientTableModel = new DefaultTableModel(columns, 0);
        clientTable = new JTable(clientTableModel);
        JScrollPane clientScroll = new JScrollPane(clientTable);

        JPanel clientPanel = new JPanel(new BorderLayout());
        clientPanel.add(new JLabel("Connected Clients"), BorderLayout.NORTH);
        clientPanel.add(clientScroll, BorderLayout.CENTER);

        disconnectButton = new JButton("Disconnect Selected");
        disconnectButton.addActionListener(e -> disconnectSelectedClient());
        clientPanel.add(disconnectButton, BorderLayout.SOUTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScroll, clientPanel);
        mainSplit.setDividerLocation(500);

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, statusScroll);
        verticalSplit.setDividerLocation(400);
        frame.add(verticalSplit, BorderLayout.CENTER);

        loadDictionaryFromDatabase();
        frame.setVisible(true);
    }

    private static void startServer() {
        if (running) return;
        running = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                appendStatus("Server đang lắng nghe cổng " + PORT + " ...");
                while (running) {
                    Socket client = serverSocket.accept();
                    appendStatus("Kết nối mới từ " + client.getInetAddress() + ":" + client.getPort());
                    ClientHandler handler = new ClientHandler(client);
                    connectedClients.add(handler);
                    updateClientTable();
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                if (running) appendStatus("Lỗi server: " + e.getMessage());
            }
        });
        serverThread.start();
    }

    private static void stopServer() {
        running = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            for (ClientHandler c : connectedClients) c.closeConnection();
            connectedClients.clear();
            updateClientTable();
            appendStatus("Server đã dừng.");
        } catch (IOException e) {
            appendStatus("Lỗi khi dừng: " + e.getMessage());
        }
    }

    private static void disconnectSelectedClient() {
        int row = clientTable.getSelectedRow();
        if (row >= 0 && row < connectedClients.size()) {
            ClientHandler handler = connectedClients.get(row);
            handler.closeConnection();
            appendStatus("Đã ngắt: " + handler.clientSocket.getInetAddress());
        }
    }

    private static void updateClientTable() {
        SwingUtilities.invokeLater(() -> {
            clientTableModel.setRowCount(0);
            for (ClientHandler c : connectedClients)
                if (!c.isClosed())
                    clientTableModel.addRow(new Object[]{
                            c.clientSocket.getInetAddress().getHostAddress(),
                            c.clientSocket.getPort(),
                            "Connected"
                    });
        });
    }

    private static void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static void appendStatus(String msg) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(msg + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    // ================== CLIENT HANDLER ==================
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private boolean closed = false;

        ClientHandler(Socket s) {
            this.clientSocket = s;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {

                String req;
                while ((req = br.readLine()) != null) {
                    appendLog("Request from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + " -> " + req);
                    if (req.equalsIgnoreCase("EXIT")) break;

                    // Đăng ký
                    if (req.startsWith("REGISTER|")) {
                        String[] parts = req.split("\\|", 4);
                        if (parts.length == 4) {
                            boolean ok = registerUser(parts[1], parts[2], parts[3]);
                            bw.write(ok ? "REGISTER_SUCCESS" : "REGISTER_FAIL");
                        } else {
                            bw.write("INVALID_FORMAT");
                        }
                        bw.newLine();
                        bw.flush();
                        continue;
                    }

                    // Đăng nhập
                    if (req.startsWith("LOGIN|")) {
                        String[] parts = req.split("\\|", 3);
                        if (parts.length == 3) {
                            String fullname = getFullNameIfLoginOK(parts[1], parts[2]);
                            bw.write(fullname != null ? "LOGIN_SUCCESS|" + fullname : "LOGIN_FAIL");
                        } else {
                            bw.write("INVALID_FORMAT");
                        }
                        bw.newLine();
                        bw.flush();
                        continue;
                    }

                    // Tra từ
                    String[] parts = req.split("\\|", 2);
                    if (parts.length != 2) {
                        bw.write("INVALID_FORMAT");
                        bw.newLine();
                        bw.flush();
                        continue;
                    }

                    String mode = parts[0], word = parts[1].toLowerCase();
                    WordEntry entry;
                    String res;
                    if (mode.equalsIgnoreCase("EN")) {
                        entry = enVi.get(word);
                        res = (entry != null) ? entry.toString() : "NOT_FOUND|" + suggest(word, enVi.keySet());
                    } else if (mode.equalsIgnoreCase("VI")) {
                        entry = viEn.get(word);
                        res = (entry != null) ? entry.toString() : "NOT_FOUND|" + suggest(word, viEn.keySet());
                    } else res = "INVALID_MODE";

                    bw.write(res);
                    bw.newLine();
                    bw.flush();
                }
            } catch (IOException e) {
                appendStatus("Client ngắt: " + clientSocket.getInetAddress() + " - " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        public void closeConnection() {
            if (closed) return;
            closed = true;
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
            connectedClients.remove(this);
            updateClientTable();
        }

        public boolean isClosed() {
            return closed;
        }
    }

    // ================== DB METHODS ==================
    private static void loadDictionaryFromDatabase() {
        enVi.clear();
        viEn.clear();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT english, vietnamese, ipa, pos, example_en, example_vi, image_path FROM dictionary")) {

            while (rs.next()) {
                String en = rs.getString("english");
                String vi = rs.getString("vietnamese");
                if (en != null) en = en.toLowerCase();
                if (vi != null) vi = vi.toLowerCase();
                String ipa = rs.getString("ipa");
                String pos = rs.getString("pos");
                String exEn = rs.getString("example_en");
                String exVi = rs.getString("example_vi");
                String img = rs.getString("image_path");

                WordEntry enEntry = new WordEntry(vi, ipa, pos, exEn, exVi, img);
                WordEntry viEntry = new WordEntry(en, ipa, pos, exEn, exVi, img);
                if (en != null) enVi.put(en, enEntry);
                if (vi != null) viEn.put(vi, viEntry);
            }
            appendStatus("Đã tải " + enVi.size() + " từ từ MySQL.");
        } catch (SQLException e) {
            appendStatus("Lỗi DB: " + e.getMessage());
        }
    }

    private static boolean registerUser(String username, String password, String fullname) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password_hash, full_name) VALUES (?, SHA2(?, 256), ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        appendStatus("Đăng ký thất bại: username tồn tại -> " + username);
                        return false;
                    }
                }
            }
            try (PreparedStatement ps2 = conn.prepareStatement(insertSql)) {
                ps2.setString(1, username);
                ps2.setString(2, password);
                ps2.setString(3, fullname);
                ps2.executeUpdate();
                appendStatus("Đăng ký thành công: " + username + " (" + fullname + ")");
                return true;
            }
        } catch (SQLException e) {
            appendStatus("Đăng ký lỗi: " + e.getMessage());
            return false;
        }
    }

    private static String getFullNameIfLoginOK(String username, String password) {
        String sql = "SELECT full_name FROM users WHERE username=? AND password_hash=SHA2(?,256)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String fullname = rs.getString("full_name");
                    return (fullname == null || fullname.trim().isEmpty()) ? username : fullname;
                }
            }
        } catch (SQLException e) {
            appendStatus("Lỗi login: " + e.getMessage());
        }
        return null;
    }

    private static String suggest(String word, Set<String> dict) {
        if (word == null || word.isEmpty()) return "Không có gợi ý";
        StringBuilder sb = new StringBuilder();
        char first = word.charAt(0);
        for (String w : dict) {
            if (w.length() > 0 && w.charAt(0) == first) {
                sb.append(w).append(", ");
            }
        }
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "Không có gợi ý";
    }

    // ================== GIAO DIỆN CHỈNH SỬA TỪ ĐIỂN ==================
    private static void openDictionaryEditor() {
        SwingUtilities.invokeLater(() -> new DictionaryEditor().setVisible(true));
    }

    static class DictionaryEditor extends JFrame {
        private JTable table;
        private DefaultTableModel model;

        DictionaryEditor() {
            setTitle("Chỉnh sửa Từ điển");
            setSize(900, 500);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            String[] cols = {"English", "Vietnamese", "IPA", "POS", "Example EN", "Example VI", "Image Path"};
            model = new DefaultTableModel(cols, 0);
            table = new JTable(model);
            loadData();

            JScrollPane scroll = new JScrollPane(table);
            add(scroll, BorderLayout.CENTER);

            JPanel buttons = new JPanel();
            JButton btnAdd = new JButton("Thêm");
            JButton btnEdit = new JButton("Sửa");
            JButton btnDelete = new JButton("Xóa");
            JButton btnReload = new JButton("Làm mới");

            buttons.add(btnAdd);
            buttons.add(btnEdit);
            buttons.add(btnDelete);
            buttons.add(btnReload);
            add(buttons, BorderLayout.SOUTH);

            btnAdd.addActionListener(e -> new WordForm(this, null).setVisible(true));
            btnEdit.addActionListener(e -> editSelectedWord());
            btnDelete.addActionListener(e -> deleteSelectedWord());
            btnReload.addActionListener(e -> loadData());
        }

        private void loadData() {
            model.setRowCount(0);
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT english, vietnamese, ipa, pos, example_en, example_vi, image_path FROM dictionary")) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("english"),
                            rs.getString("vietnamese"),
                            rs.getString("ipa"),
                            rs.getString("pos"),
                            rs.getString("example_en"),
                            rs.getString("example_vi"),
                            rs.getString("image_path")
                    });
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage());
            }
        }

        private void editSelectedWord() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Chọn từ cần sửa!");
                return;
            }
            String[] data = new String[7];
            for (int i = 0; i < 7; i++) {
                data[i] = model.getValueAt(row, i).toString();
            }
            new WordForm(this, data).setVisible(true);
        }

        private void deleteSelectedWord() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Chọn từ cần xóa!");
                return;
            }
            String english = model.getValueAt(row, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa từ '" + english + "'?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM dictionary WHERE english=?")) {
                    ps.setString(1, english);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Đã xóa thành công!");
                    loadData();
                    loadDictionaryFromDatabase();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage());
                }
            }
        }
    }

    static class WordForm extends JFrame {
        private JTextField txtEn, txtVi, txtIpa, txtPos, txtExEn, txtExVi, txtImg;
        private boolean isEdit = false;
        private String oldEnglish;
        private DictionaryEditor parent;

        WordForm(DictionaryEditor parent, String[] data) {
            this.parent = parent;
            setTitle(data == null ? "Thêm từ mới" : "Sửa từ");
            setSize(500, 400);
            setLocationRelativeTo(null);
            setLayout(new GridLayout(8, 2, 10, 10));
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            txtEn = new JTextField();
            txtVi = new JTextField();
            txtIpa = new JTextField();
            txtPos = new JTextField();
            txtExEn = new JTextField();
            txtExVi = new JTextField();
            txtImg = new JTextField();

            add(new JLabel("English:")); add(txtEn);
            add(new JLabel("Vietnamese:")); add(txtVi);
            add(new JLabel("IPA:")); add(txtIpa);
            add(new JLabel("POS:")); add(txtPos);
            add(new JLabel("Example EN:")); add(txtExEn);
            add(new JLabel("Example VI:")); add(txtExVi);
            add(new JLabel("Image Path:")); add(txtImg);

            JButton btnSave = new JButton("Lưu");
            JButton btnCancel = new JButton("Hủy");
            add(btnSave); add(btnCancel);

            if (data != null) {
                isEdit = true;
                oldEnglish = data[0];
                txtEn.setText(data[0]);
                txtVi.setText(data[1]);
                txtIpa.setText(data[2]);
                txtPos.setText(data[3]);
                txtExEn.setText(data[4]);
                txtExVi.setText(data[5]);
                txtImg.setText(data[6]);
            }

            btnSave.addActionListener(e -> saveWord());
            btnCancel.addActionListener(e -> dispose());
        }

        private void saveWord() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = isEdit
                        ? "UPDATE dictionary SET english=?, vietnamese=?, ipa=?, pos=?, example_en=?, example_vi=?, image_path=? WHERE english=?"
                        : "INSERT INTO dictionary (english, vietnamese, ipa, pos, example_en, example_vi, image_path) VALUES (?,?,?,?,?,?,?)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, txtEn.getText().trim());
                    ps.setString(2, txtVi.getText().trim());
                    ps.setString(3, txtIpa.getText().trim());
                    ps.setString(4, txtPos.getText().trim());
                    ps.setString(5, txtExEn.getText().trim());
                    ps.setString(6, txtExVi.getText().trim());
                    ps.setString(7, txtImg.getText().trim());
                    if (isEdit) ps.setString(8, oldEnglish);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Đã lưu thành công!");
                parent.loadData();
                loadDictionaryFromDatabase();
                dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi lưu: " + ex.getMessage());
            }
        }
    }
}
