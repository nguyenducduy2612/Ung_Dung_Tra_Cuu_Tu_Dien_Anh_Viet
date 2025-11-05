<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   ỨNG DỤNG TRA CỨU TỪ ĐIỂN ANH-VIỆT
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)
</div>

## 1. Giới thiệu

Ứng dụng Tra Cứu Từ Điển Anh - Việt là một dự án học tập được phát triển bằng **Java Socket** và **Java Swing** trong khuôn khổ nghiên cứu tại **AIoTLab**, Khoa Công Nghệ Thông Tin, Đại học Đại Nam. Ứng dụng cung cấp một hệ thống client-server cho phép tra cứu từ vựng song ngữ (Tiếng Anh ↔ Tiếng Việt) với giao diện người dùng thân thiện và khả năng xử lý đa luồng hiệu quả.
### Cấu trúc ứng dụng:
- **Server**: 
  - Quản lý dữ liệu từ điển từ cơ sở dữ liệu `dictionarydb` hoặc sử dụng dữ liệu mặc định (apple, book, dog).
  - Hỗ trợ đa luồng để xử lý nhiều kết nối từ client đồng thời.
  - Chạy trên cổng 2000, lắng nghe các yêu cầu tra cứu từ client.
  - Giao diện quản lý tử điển dễ dùng, có thể thêm sửa xóa từ, quản lý client truy cập.
- **Client**: 
  - Giao diện đồ họa (GUI) được xây dựng bằng Java Swing.
  - database để mở rộng từ điển.

3. **Biên dịch mã nguồn**:
   - Mở terminal/command prompt, di chuyển đến thư mục dự án:
     ```
     cd tudien
     ```
   - Biên dịch cả hai file:
     ```
     javac tudien/*.java
     ```
   - Kiểm tra lỗi biên dịch (thường do thiếu JDK hoặc cấu hình sai).

4. **Chạy ứng dụng**:
   - **Khởi động Server**:
     ```
     java tudien.Server
     ```
     - Server sẽ chạy trên cổng 2000, tải dữ liệu từ điển và hiển thị số lượng từ đã tải.
   - **Khởi động Client**:
     ```
     java tudien.Client
     ```
     - Giao diện client sẽ xuất hiện và tự động kết nối đến `localhost:2000`.

5. **Kiểm tra kết nối**:
   - Nếu client hiển thị trạng thái **"Đã kết nối tới server!"** (màu xanh), ứng dụng đã sẵn sàng.
   - Nếu gặp lỗi (ví dụ: "Không kết nối được server!"), kiểm tra:
     - Server đã chạy chưa.
     - Firewall có chặn cổng 2000 không.
     - Địa chỉ `localhost` trong `Client.java` có đúng không (nếu chạy trên mạng, thay bằng IP server).

### Hướng dẫn sử dụng
1. **Khởi động ứng dụng**:
   - Chạy Server trước để lắng nghe kết nối.
   - Mở Client, giao diện sẽ hiển thị trạng thái kết nối.

2. **Tra cứu từ**:
   - Chọn chế độ tra cứu (**Tiếng Anh** hoặc **Tiếng Việt**) từ combobox.
   - Nhập từ cần tra vào ô **"Nhập từ / câu"**.
   - Nhấn nút **"Tra cứu"**.
   - Kết quả hiển thị ở các ô:
     - **Bản dịch**: Nghĩa của từ.
     - **Phiên âm**: Phát âm theo chuẩn IPA.
     - **Từ loại**: Danh từ, động từ, tính từ, v.v.
     - **Ví dụ (Tiếng Anh)**: Câu ví dụ bằng tiếng Anh.
     - **Ví dụ (Tiếng Việt)**: Câu ví dụ bằng tiếng Việt.
       - **Hình ảnh minh họa**:Hình ảnh minh họa tương ứng với từ được tra.
   - Nếu không tìm thấy từ, ô **"Gợi ý"** sẽ hiển thị các từ bắt đầu bằng ký tự đầu tiên.

3. **Thoát ứng dụng**:
   - Đóng cửa sổ Client sẽ gửi lệnh `EXIT` đến Server và ngắt kết nối.
   - Dừng Server bằng phím `Ctrl+C` trong terminal.

### Lưu ý:
- Ứng dụng hiện chỉ hỗ trợ tra cứu từ đơn. Để hỗ trợ cụm từ hoặc câu, cần nâng cấp logic xử lý.
- Để triển khai trên mạng, chỉnh sửa `localhost` trong `Client.java` thành địa chỉ IP của server.
- Nếu gặp lỗi hiển thị tiếng Việt, kiểm tra encoding của tệp CSV (phải là UTF-8).

## 5. Thông tin liên hệ

- **Họ tên**: Nguyễn Đức Duy  
- **Lớp**: CNTT 16-01  
- **Email**: [Nguyenducduy2612@icloud.com](mailto:Nguyenducduy2612@icloud.com)  
- **GitHub**: [github.com/nguyenducduy2612/Ung_Dung_Tra_Cuu_Tu_Dien_Anh_Viet](github.com/nguyenducduy2612/Ung_Dung_Tra_Cuu_Tu_Dien_Anh_Viet) 
- **Phòng thí nghiệm**: AIoTLab, Khoa Công Nghệ Thông Tin, Đại học Đại Nam  
- **Website**: [dainam.edu.vn](https://dainam.edu.vn)  
- **Facebook AIoTLab**: [facebook.com/DNUAIoTLab](https://www.facebook.com/DNUAIoTLab)

Nếu bạn có câu hỏi, gặp lỗi, hoặc muốn đề xuất tính năng mới (ví dụ: hỗ trợ tra cứu trực tuyến, thêm ngôn ngữ khác), hãy liên hệ qua email hoặc mở issue trên GitHub. Mọi đóng góp đều được hoan nghênh!

© 2025 AIoTLab, Khoa Công Nghệ Thông Tin, Đại học Đại Nam.
