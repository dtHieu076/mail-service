**./mvnw clean package -Pnative -DskipTests**

=> Trong file cấu hình có: quarkus.native.container-build=true

=> `quarkus.native.container-build=true` để xây dựng file thực thi native.

- Không cần cài đặt GraalVM: Thay vì phải thiết lập thủ công GraalVM và `native-image` trên máy thật, Quarkus sẽ sử dụng một Builder Image có sẵn (thông qua Docker).
- Tính tương thích hệ điều hành: Native Image là mã máy trực tiếp, không phụ thuộc vào JVM. Do đó, việc build trong container giúp tạo ra file thực thi định dạng Linux (ngay cả khi bạn đang dùng Windows/macOS)

**docker build -f src/main/docker/Dockerfile.native -t dthieudocker/mail-quarkus-backend:3.0 .**

**docker push dthieudocker/mail-quarkus-backend:3.0**
