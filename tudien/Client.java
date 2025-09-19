package tudien;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private JTextArea txtInput, txtMeaning, txtIpa, txtPartOfSpeech, txtExampleEn, txtExampleVi, txtSuggestions;
    private JLabel lblStatus;
    private JComboBox<String> cbMode;
    private BufferedReader br;
    private BufferedWriter bw;
    private Socket socket;

    public Client() {
        setTitle("Từ điển Anh - Việt");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Font fontLabel = new Font("Segoe UI", Font.BOLD, 16);
        Font fontText = new Font("Segoe UI", Font.PLAIN, 15);

        // ===== Top panel =====
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        top.setBackground(new Color(245, 250, 255));

        JLabel lblMode = new JLabel("Chế độ: ");
        lblMode.setFont(fontLabel);

        cbMode = new JComboBox<>(new String[]{"Tiếng Anh", "Tiếng Việt"});
        cbMode.setFont(fontText);
        cbMode.setPreferredSize(new Dimension(150, 36));

        JButton btnSearch = new JButton("Tra cứu");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnSearch.setFocusPainted(false);
        btnSearch.setPreferredSize(new Dimension(120, 36));
        btnSearch.setBackground(new Color(70, 150, 250));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setBorder(new LineBorder(new Color(60, 120, 220), 1, true));

        btnSearch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnSearch.setBackground(new Color(40, 120, 220));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnSearch.setBackground(new Color(70, 150, 250));
            }
        });

        lblStatus = new JLabel("Chưa kết nối", SwingConstants.CENTER);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(250, 240, 200));
        lblStatus.setForeground(Color.BLACK);
        lblStatus.setPreferredSize(new Dimension(250, 36));
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblStatus.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));

        top.add(lblMode);
        top.add(cbMode);
        top.add(btnSearch);
        top.add(lblStatus);

        // ===== Panel dịch (Nhập & Nghĩa) =====
        JPanel translatePanel = new JPanel(new GridBagLayout());
        translatePanel.setBackground(new Color(245, 250, 255));
        GridBagConstraints gbcT = new GridBagConstraints();
        gbcT.insets = new Insets(10, 20, 10, 20);
        gbcT.fill = GridBagConstraints.BOTH;
        gbcT.weightx = 1.0;
        gbcT.weighty = 1.0;

        txtInput = makeTextArea(7, fontText, true);
        JScrollPane scrollInput = createScrollPane(txtInput, "Nhập từ / câu", fontLabel);
        gbcT.gridx = 0;
        gbcT.gridy = 0;
        translatePanel.add(scrollInput, gbcT);

        txtMeaning = makeTextArea(7, fontText, false);
        JScrollPane scrollMeaning = createScrollPane(txtMeaning, "Bản dịch", fontLabel);
        gbcT.gridx = 1;
        gbcT.gridy = 0;
        translatePanel.add(scrollMeaning, gbcT);

        // ===== Panel kết quả chi tiết =====
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBackground(new Color(245, 250, 255));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 100, 20, 100));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0;

        int row = 0;
        txtIpa = addField(bottomPanel, "Phiên âm:", 1, row++, gbc, fontLabel, fontText);
        txtPartOfSpeech = addField(bottomPanel, "Từ loại:", 1, row++, gbc, fontLabel, fontText);
        txtExampleEn = addField(bottomPanel, "Ví dụ (Tiếng Anh):", 2, row++, gbc, fontLabel, fontText);
        txtExampleVi = addField(bottomPanel, "Ví dụ (Tiếng Việt):", 2, row++, gbc, fontLabel, fontText);
        txtSuggestions = addField(bottomPanel, "Gợi ý:", 2, row++, gbc, fontLabel, fontText);

        // ===== Layout chính =====
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 250, 255));
        mainPanel.add(top, BorderLayout.NORTH);
        mainPanel.add(translatePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // ===== Sự kiện =====
        btnSearch.addActionListener(e -> searchWord());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                closeConnection();
                System.exit(0);
            }
        });

        connectServer();
    }

    private JScrollPane createScrollPane(JTextArea area, String title, Font fontLabel) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(350, 180));
        scroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(180, 180, 180), 1, true),
                title, 0, 0, fontLabel));
        return scroll;
    }

    private JTextArea makeTextArea(int rows, Font font, boolean editable) {
        JTextArea area = new JTextArea(rows, 20);
        area.setEditable(editable);
        area.setFont(font);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(Color.WHITE);
        area.setForeground(Color.BLACK);
        area.setBorder(new EmptyBorder(5, 5, 5, 5));
        return area;
    }

    private JTextArea addField(JPanel panel, String label, int rows, int row, GridBagConstraints gbc, Font fontLabel, Font fontText) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel lbl = new JLabel(label, SwingConstants.RIGHT);
        lbl.setFont(fontLabel);
        panel.add(lbl, gbc);

        JTextArea area = makeTextArea(rows, fontText, false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, rows * 35));
        scroll.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(scroll, gbc);

        return area;
    }

    private void connectServer() {
        try {
            socket = new Socket("localhost", 2000);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            showStatus("Đã kết nối tới server!", new Color(220, 255, 220));
        } catch (IOException e) {
            showStatus("Không kết nối được server!", new Color(255, 220, 220));
            JOptionPane.showMessageDialog(this, "Không kết nối được server: " + e.getMessage());
            System.exit(0);
        }
    }

    private void searchWord() {
        try {
            String mode = cbMode.getSelectedItem().toString().equals("Tiếng Anh") ? "EN" : "VI";
            String word = txtInput.getText().trim();
            if (word.isEmpty()) {
                showStatus("Vui lòng nhập từ cần tra cứu!", new Color(255, 240, 200));
                clearResults();
                return;
            }

            bw.write(mode + "|" + word);
            bw.newLine();
            bw.flush();

            String response = br.readLine();
            if (response == null) {
                showStatus("Mất kết nối với server!", new Color(255, 220, 220));
                clearResults();
                closeConnection();
                return;
            }

            if (response.equals("INVALID_FORMAT")) {
                showStatus("Định dạng yêu cầu không hợp lệ!", new Color(255, 220, 220));
                clearResults();
            } else if (response.equals("INVALID_MODE")) {
                showStatus("Chế độ tra cứu không hợp lệ!", new Color(255, 220, 220));
                clearResults();
            } else {
                String[] parts = response.split("\\|", 5);
                if (parts[0].equals("NOT_FOUND")) {
                    showStatus("Không tìm thấy từ: " + word, new Color(255, 220, 220));
                    clearResults();
                    txtSuggestions.setText(parts.length > 1 ? parts[1] : "");
                } else {
                    showStatus("Tra cứu thành công!", new Color(220, 255, 220));
                    txtMeaning.setText(parts[0]);
                    txtIpa.setText(parts.length > 1 ? parts[1] : "");
                    txtPartOfSpeech.setText(parts.length > 2 ? parts[2] : "");
                    txtExampleEn.setText(parts.length > 3 ? parts[3] : "");
                    txtExampleVi.setText(parts.length > 4 ? parts[4] : "");
                    txtSuggestions.setText("");
                }
            }
        } catch (IOException e) {
            showStatus("Lỗi khi gửi/nhận dữ liệu!", new Color(255, 220, 220));
            clearResults();
            closeConnection();
        }
    }

    private void clearResults() {
        txtMeaning.setText("");
        txtIpa.setText("");
        txtPartOfSpeech.setText("");
        txtExampleEn.setText("");
        txtExampleVi.setText("");
        txtSuggestions.setText("");
    }

    private void showStatus(String message, Color bgColor) {
        lblStatus.setText(message);
        lblStatus.setBackground(bgColor);
        lblStatus.setForeground(Color.BLACK);
        lblStatus.setBorder(new LineBorder(new Color(180, 180, 180), 1, true));
    }

    private void closeConnection() {
        try {
            if (bw != null) {
                bw.write("EXIT");
                bw.newLine();
                bw.flush();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}
