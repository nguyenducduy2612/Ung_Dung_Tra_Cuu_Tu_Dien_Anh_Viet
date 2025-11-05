package tudien;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Giao diện chính của từ điển Anh-Việt có đăng nhập, đăng ký
 */
public class Client extends JFrame {
    private JTextArea txtInput, txtMeaning, txtIpa, txtPartOfSpeech, txtExampleEn, txtExampleVi, txtSuggestions, txtHistory;
    private JLabel lblStatus, lblImage;
    private JComboBox<String> cbMode;
    private BufferedReader br;
    private BufferedWriter bw;
    private Socket socket;
    private String username, fullname;

    public Client(String username, String fullname, Socket socket, BufferedReader br, BufferedWriter bw) {
        this.username = username;
        this.fullname = fullname;
        this.socket = socket;
        this.br = br;
        this.bw = bw;

        setTitle("Từ điển Anh - Việt (" + username + ")");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        Font fontLabel = new Font("Segoe UI", Font.BOLD, 18);
        Font fontText = new Font("Segoe UI", Font.PLAIN, 17);

        // ===== Thanh trên cùng =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        top.setBackground(new Color(240, 245, 250));

        JLabel lblMode = new JLabel("Chế độ:");
        lblMode.setFont(fontLabel);

        cbMode = new JComboBox<>(new String[]{"Tiếng Anh", "Tiếng Việt"});
        cbMode.setFont(fontText);
        cbMode.setPreferredSize(new Dimension(170, 40));

        JButton btnSearch = makeStyledButton("Tra cứu", new Color(70, 150, 250), new Color(40, 120, 220));
        JButton btnClear = makeStyledButton("Xóa", new Color(230, 70, 70), new Color(200, 50, 50));

        lblStatus = new JLabel("Xin chào, " + fullname , SwingConstants.CENTER);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(220, 255, 220));
        lblStatus.setPreferredSize(new Dimension(500, 40));
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblStatus.setBorder(new LineBorder(new Color(180, 180, 180), 1, true));

        top.add(lblMode);
        top.add(cbMode);
        top.add(btnSearch);
        top.add(btnClear);
        top.add(lblStatus);

        // ===== Khu vực chính =====
        txtInput = makeTextArea(8, fontText, true);
        txtMeaning = makeTextArea(8, fontText, false);
        txtIpa = makeTextArea(2, fontText, false);
        txtPartOfSpeech = makeTextArea(2, fontText, false);
        txtExampleEn = makeTextArea(3, fontText, false);
        txtExampleVi = makeTextArea(3, fontText, false);
        txtSuggestions = makeTextArea(3, fontText, false);
        txtHistory = makeTextArea(35, fontText, false);

        JScrollPane scrollInput = createScrollPane(txtInput, "Từ cần tra", fontLabel);
        JScrollPane scrollMeaning = createScrollPane(txtMeaning, "Bản dịch", fontLabel);
        JScrollPane scrollHistory = createScrollPane(txtHistory, "Lịch sử tra cứu", fontLabel);
        scrollHistory.setPreferredSize(new Dimension(300, 700));

        JPanel translatePanel = new JPanel(new GridLayout(1, 2, 20, 10));
        translatePanel.add(scrollInput);
        translatePanel.add(scrollMeaning);

        lblImage = new JLabel("Ảnh minh họa", SwingConstants.CENTER);
        lblImage.setPreferredSize(new Dimension(250, 250));
        lblImage.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));

        JPanel infoPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoPanel.add(new JLabel("Phiên âm:")); infoPanel.add(new JScrollPane(txtIpa));
        infoPanel.add(new JLabel("Từ loại:")); infoPanel.add(new JScrollPane(txtPartOfSpeech));
        infoPanel.add(new JLabel("Ví dụ (EN):")); infoPanel.add(new JScrollPane(txtExampleEn));
        infoPanel.add(new JLabel("Ví dụ (VI):")); infoPanel.add(new JScrollPane(txtExampleVi));
        infoPanel.add(new JLabel("Gợi ý:")); infoPanel.add(new JScrollPane(txtSuggestions));

        JPanel detailPanel = new JPanel(new BorderLayout(10, 10));
        detailPanel.add(lblImage, BorderLayout.WEST);
        detailPanel.add(infoPanel, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(15, 15));
        center.add(translatePanel, BorderLayout.NORTH);
        center.add(detailPanel, BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.add(top, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        main.add(scrollHistory, BorderLayout.EAST);
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(main);

        // ===== Sự kiện =====
        btnSearch.addActionListener(e -> searchWord());
        btnClear.addActionListener(e -> clearResults());
        txtInput.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) searchWord();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeConnection();
                System.exit(0);
            }
        });
    }

    private JButton makeStyledButton(String text, Color bg, Color hover) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private JScrollPane createScrollPane(JTextArea area, String title, Font fontLabel) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return scroll;
    }

    private JTextArea makeTextArea(int rows, Font font, boolean editable) {
        JTextArea area = new JTextArea(rows, 20);
        area.setFont(font);
        area.setEditable(editable);
        return area;
    }

    private void searchWord() {
        try {
            String mode = cbMode.getSelectedItem().toString().equals("Tiếng Anh") ? "EN" : "VI";
            String word = txtInput.getText().trim();

            if (word.isEmpty()) {
                showStatus("Vui lòng nhập từ!", new Color(255, 250, 200));
                clearResults();
                return;
            }

            bw.write(mode + "|" + word);
            bw.newLine(); bw.flush();

            String response = br.readLine();
            if (response == null) return;

            if (response.startsWith("NOT_FOUND")) {
                String[] parts = response.split("\\|", 2);
                showStatus("Không tìm thấy từ!", new Color(255, 220, 220));
                txtMeaning.setText("");
                txtIpa.setText("");
                txtPartOfSpeech.setText("");
                txtExampleEn.setText("");
                txtExampleVi.setText("");
                txtSuggestions.setText(parts.length > 1 ? parts[1] : "Không có gợi ý");
                lblImage.setIcon(null);
                lblImage.setText("Không có ảnh minh họa");
                return;
            }

            // Đọc dữ liệu hợp lệ
            String[] parts = response.split("\\|", 6);
            txtMeaning.setText(parts[0]);
            txtIpa.setText(parts.length > 1 ? parts[1] : "");
            txtPartOfSpeech.setText(parts.length > 2 ? parts[2] : "");
            txtExampleEn.setText(parts.length > 3 ? parts[3] : "");
            txtExampleVi.setText(parts.length > 4 ? parts[4] : "");
            txtSuggestions.setText("");
            String img = parts.length > 5 ? parts[5] : "";
            setImageFromPath(img);

            showStatus("Đã hiển thị kết quả cho \"" + word + "\"", new Color(220, 255, 220));
            txtHistory.append(word + " → " + parts[0] + "\n");

        } catch (IOException e) {
            showStatus("Lỗi kết nối với máy chủ!", new Color(255, 200, 200));
        }
    }

    private void setImageFromPath(String path) {
        if (path == null || path.isEmpty()) {
            lblImage.setIcon(null);
            lblImage.setText("Không có ảnh minh họa");
            return;
        }
        try {
            BufferedImage img = path.startsWith("http") ? ImageIO.read(new URL(path)) : ImageIO.read(new File(path));
            if (img != null) {
                lblImage.setIcon(new ImageIcon(img.getScaledInstance(250, 250, Image.SCALE_SMOOTH)));
                lblImage.setText("");
            } else {
                lblImage.setText("Không có ảnh minh họa");
            }
        } catch (Exception e) {
            lblImage.setIcon(null);
            lblImage.setText("Không có ảnh minh họa");
        }
    }

    private void clearResults() {
        txtMeaning.setText(""); txtIpa.setText(""); txtPartOfSpeech.setText("");
        txtExampleEn.setText(""); txtExampleVi.setText(""); txtSuggestions.setText("");
        lblImage.setIcon(null); lblImage.setText("Ảnh minh họa");
        showStatus("Đã xóa nội dung tra cứu", new Color(240, 245, 250));
    }

    private void showStatus(String msg, Color bg) {
        lblStatus.setText(msg);
        lblStatus.setBackground(bg);
    }

    private void closeConnection() {
        try {
            bw.write("EXIT"); bw.newLine(); bw.flush();
            socket.close();
        } catch (IOException ignored) {}
    }

    // ==================== MỞ ỨNG DỤNG ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}

/** Giao diện đăng nhập */
class LoginForm extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;

    public LoginForm() {
        setTitle("Đăng nhập");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtUser = new JTextField();
        txtPass = new JPasswordField();

        JButton btnLogin = new JButton("Đăng nhập");
        JButton btnRegister = new JButton("Đăng ký");

        panel.add(new JLabel("Tài khoản:")); panel.add(txtUser);
        panel.add(new JLabel("Mật khẩu:")); panel.add(txtPass);
        panel.add(btnLogin); panel.add(btnRegister);

        add(panel);

        btnLogin.addActionListener(e -> login());
        btnRegister.addActionListener(e -> {
            dispose();
            new RegisterForm().setVisible(true);
        });
    }

    private void login() {
        try {
            Socket socket = new Socket("localhost", 2000);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());

            bw.write("LOGIN|" + user + "|" + pass);
            bw.newLine(); bw.flush();

            String res = br.readLine();
            if (res != null && res.startsWith("LOGIN_SUCCESS")) {
                String[] parts = res.split("\\|", 2);
                String fullname = parts.length > 1 ? parts[1] : user;
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");
                dispose();
                new Client(user, fullname, socket, br, bw).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
                socket.close();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server!");
        }
    }
}

/** Giao diện đăng ký */
class RegisterForm extends JFrame {
    private JTextField txtUser, txtName;
    private JPasswordField txtPass;

    public RegisterForm() {
        setTitle("Đăng ký tài khoản");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtUser = new JTextField();
        txtPass = new JPasswordField();
        txtName = new JTextField();

        JButton btnRegister = new JButton("Đăng ký");
        JButton btnBack = new JButton("Quay lại đăng nhập");

        panel.add(new JLabel("Tài khoản:")); panel.add(txtUser);
        panel.add(new JLabel("Mật khẩu:")); panel.add(txtPass);
        panel.add(new JLabel("Họ tên:")); panel.add(txtName);
        panel.add(btnRegister); panel.add(btnBack);
        add(panel);

        btnRegister.addActionListener(e -> register());
        btnBack.addActionListener(e -> {
            dispose();
            new LoginForm().setVisible(true);
        });
    }

    private void register() {
        try {
            Socket socket = new Socket("localhost", 2000);
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            String name = txtName.getText().trim();

            if (user.isEmpty() || pass.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            bw.write("REGISTER|" + user + "|" + pass + "|" + name);
            bw.newLine(); bw.flush();

            String res = br.readLine();
            if ("REGISTER_SUCCESS".equals(res)) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Mời đăng nhập.");
                dispose();
                new LoginForm().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Tên tài khoản đã tồn tại!");
            }
            socket.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server!");
        }
    }
}