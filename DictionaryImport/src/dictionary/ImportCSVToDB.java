package dictionary;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ImportCSVToDB {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/dictionarydb?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String DB_USER = "root";        // đổi nếu bạn có user khác
    private static final String DB_PASS = "MySql@123";      // đổi nếu mật khẩu khác
    private static final String CSV_FILE = "english-vietnamese.csv"; // đặt trong project
    private static final int BATCH_SIZE = 500;

    public static void main(String[] args) {
        importCsv();
    }

    private static void importCsv() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            System.out.println("✅ Đã kết nối MySQL thành công!");

            ensureTableExists(conn);

            String sql = "INSERT INTO dictionary (english, vietnamese, ipa, pos, example_en, example_vi, image_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(CSV_FILE), StandardCharsets.UTF_8))) {

                String header = reader.readLine(); // bỏ header
                if (header == null) {
                    System.out.println("⚠️ File CSV rỗng hoặc không tồn tại.");
                    return;
                }

                String line;
                int count = 0;
                int total = 0;

                while ((line = reader.readLine()) != null) {
                    List<String> parts = parseCSVLine(line);
                    if (parts.size() < 2) continue;

                    String english = parts.size() > 0 ? parts.get(0).trim() : "";
                    String vietnamese = parts.size() > 1 ? parts.get(1).trim() : "";
                    String ipa = parts.size() > 2 ? parts.get(2).trim() : "";
                    String pos = parts.size() > 3 ? parts.get(3).trim() : "";
                    String exampleEn = parts.size() > 4 ? parts.get(4).trim() : "";
                    String exampleVi = parts.size() > 5 ? parts.get(5).trim() : "";
                    String imagePath = parts.size() > 6 ? parts.get(6).trim() : "";

                    if (english.isEmpty() && vietnamese.isEmpty()) continue;

                    ps.setString(1, english);
                    ps.setString(2, vietnamese);
                    ps.setString(3, ipa);
                    ps.setString(4, pos);
                    ps.setString(5, exampleEn);
                    ps.setString(6, exampleVi);
                    ps.setString(7, imagePath);
                    ps.addBatch();
                    count++;

                    if (count >= BATCH_SIZE) {
                        ps.executeBatch();
                        conn.commit();
                        total += count;
                        System.out.println("📥 Imported " + total + " rows...");
                        count = 0;
                    }
                }

                if (count > 0) {
                    ps.executeBatch();
                    conn.commit();
                    total += count;
                }
                System.out.println("✅ Import completed, total " + total + " rows imported!");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) return result;
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString());
        return result;
    }

    private static void ensureTableExists(Connection conn) throws SQLException {
        String create = "CREATE TABLE IF NOT EXISTS dictionary (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "english VARCHAR(100) NOT NULL," +
                "vietnamese VARCHAR(100) NOT NULL," +
                "ipa VARCHAR(50)," +
                "pos VARCHAR(50)," +
                "example_en TEXT," +
                "example_vi TEXT," +
                "image_path VARCHAR(255)," +
                "INDEX idx_english (english)," +
                "INDEX idx_vietnamese (vietnamese)" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(create);
        }
    }
}

