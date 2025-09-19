package tudien;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 2000;
    private static Map<String, WordEntry> enVi = new HashMap<>();
    private static Map<String, WordEntry> viEn = new HashMap<>();
    static class WordEntry {
        String meaning;
        String ipa;
        String partOfSpeech;
        String exampleEn;
        String exampleVi;

        WordEntry(String meaning, String ipa, String partOfSpeech, String exampleEn, String exampleVi) {
            this.meaning = meaning;
            this.ipa = ipa;
            this.partOfSpeech = partOfSpeech;
            this.exampleEn = exampleEn;
            this.exampleVi = exampleVi;
        }

        @Override
        public String toString() {
            return meaning + "|" + ipa + "|" + partOfSpeech + "|" + exampleEn + "|" + exampleVi;
        }
    }

    public static void main(String[] args) {
        loadDictionaryFromFile("english-vietnamese.csv");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe trên cổng " + PORT + " ...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Kết nối thành công từ " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String request;
            while ((request = br.readLine()) != null) {
                System.out.println("Client: " + request);
                if (request.equalsIgnoreCase("EXIT")) {
                    break;
                }

                String[] parts = request.split("\\|");
                if (parts.length != 2) {
                    bw.write("INVALID_FORMAT");
                    bw.newLine();
                    bw.flush();
                    continue;
                }

                String mode = parts[0];
                String word = parts[1].toLowerCase();
                String response;

                if (mode.equalsIgnoreCase("EN")) {
                    WordEntry entry = enVi.get(word);
                    if (entry == null) {
                        response = "NOT_FOUND|" + suggest(word, enVi.keySet());
                    } else {
                        response = entry.toString();
                    }
                } else if (mode.equalsIgnoreCase("VI")) {
                    WordEntry entry = viEn.get(word);
                    if (entry == null) {
                        response = "NOT_FOUND|" + suggest(word, viEn.keySet());
                    } else {
                        response = entry.toString();
                    }
                } else {
                    response = "INVALID_MODE";
                }

                bw.write(response);
                bw.newLine();
                bw.flush();
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối");
        }
    }

    private static void loadDictionaryFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String line;
            reader.readLine(); // Bỏ qua header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    String enWord = parts[0].trim().toLowerCase();
                    String viMeaning = parts[1].trim().toLowerCase();
                    String ipa = parts[2].trim();
                    String partOfSpeech = parts[3].trim().toLowerCase();
                    String exampleEn = parts[4].trim();
                    String exampleVi = parts[5].trim();
                    enVi.put(enWord, new WordEntry(viMeaning, ipa, partOfSpeech, exampleEn, exampleVi));
                    viEn.put(viMeaning, new WordEntry(enWord, ipa, partOfSpeech, exampleEn, exampleVi));
                }
            }
            System.out.println("Đã tải " + enVi.size() + " từ vào từ điển.");
        } catch (IOException e) {
            System.err.println("Lỗi đọc file từ điển: " + e.getMessage());
            // Thêm dữ liệu mặc định
            enVi.put("apple", new WordEntry("quả táo", "/ˈæp.l̩/", "danh từ", "I ate an apple for breakfast.", "Tôi ăn một quả táo vào bữa sáng."));
            enVi.put("book", new WordEntry("cuốn sách", "/bʊk/", "danh từ", "She is reading a book in the library.", "Cô ấy đang đọc một cuốn sách trong thư viện."));
            enVi.put("dog", new WordEntry("con chó", "/dɒɡ/", "danh từ", "The dog is playing in the yard.", "Con chó đang chơi ngoài sân."));
            viEn.put("táo", new WordEntry("apple", "/ˈæp.l̩/", "danh từ", "I ate an apple for breakfast.", "Tôi ăn một quả táo vào bữa sáng."));
            viEn.put("sách", new WordEntry("book", "/bʊk/", "danh từ", "She is reading a book in the library.", "Cô ấy đang đọc một cuốn sách trong thư viện."));
            viEn.put("chó", new WordEntry("dog", "/dɒɡ/", "danh từ", "The dog is playing in the yard.", "Con chó đang chơi ngoài sân."));
        }
    }

    private static String suggest(String word, Set<String> dict) {
        StringBuilder sb = new StringBuilder();
        if (word.length() > 0) {
            for (String w : dict) {
                if (w.startsWith(String.valueOf(word.charAt(0)))) {
                    sb.append(w).append(", ");
                }
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "Không có gợi ý";
    }
}