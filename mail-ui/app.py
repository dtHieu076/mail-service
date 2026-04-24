import streamlit as st
from websocket import create_connection
from kafka import KafkaProducer
import json
import time
import math

# --- 1. CẤU HÌNH & KHỞI TẠO ---

def init_kafka_producer():
    if 'kafka_producer' not in st.session_state:
        try:
            st.session_state.kafka_producer = KafkaProducer(
                bootstrap_servers=['localhost:9092'],
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                acks='all'
            )
        except Exception as e:
            st.error(f"Lỗi kết nối Kafka: {e}")
            return None
    return st.session_state.kafka_producer

def connect_ws():
    """Hàm này chủ động tạo kết nối mới hoặc trả về kết nối cũ"""
    try:
        # Nếu đã có kết nối và còn sống thì dùng tiếp
        if "ws" in st.session_state and st.session_state.ws.connected:
            return st.session_state.ws
        
        # Nếu chưa có hoặc đã chết, tạo mới
        st.session_state.ws = create_connection("ws://localhost:8080/mail-status", timeout=10)
        return st.session_state.ws
    except Exception as e:
        st.error(f"Không thể kết nối WebSocket tới Quarkus: {e}")
        return None

# --- 2. LOGIC NGHIỆP VỤ ---

def send_batch_to_kafka(total_emails, batch_size):
    """Chia batch và đẩy dữ liệu vào Kafka theo cấu trúc template"""
    producer = st.session_state.kafka_producer
    if not producer:
        st.error("Kafka Producer chưa khởi tạo!")
        return

    progress_bar = st.progress(0)
    status_text = st.empty()
    sent_count = 0

    # Chạy vòng lặp theo từng bước nhảy của batch_size
    for i in range(0, total_emails, batch_size):
        # Xác định số lượng email thực tế trong batch này (tránh dư ở cuối)
        end_idx = min(i + batch_size, total_emails)
        
        # Tạo danh sách người nhận cho batch hiện tại
        recipients_list = []
        for stt in range(i + 1, end_idx + 1):
            recipients_list.append({
                "email": f"user{stt}@atlas-erp.com",
                "name": f"Người dùng số {stt}"
            })
        
        # Tạo Payload theo cấu trúc bạn yêu cầu
        payload = {
            "templateId": "welcome_email",
            "templateData": {
                "senderName": "Đặng Thanh Hiếu",
                "holidayName": "Tết Nguyên Đán",
                "date": "15/01/2026",
                "company": "Atlas ERP System"
            },
            "recipients": recipients_list,
            "tags": ["holiday", "announcement", "2026"]
        }

        # Đẩy nguyên cái batch (payload) này vào Kafka
        producer.send('mail_requests_1', payload)
        producer.flush() 

        sent_count = end_idx
        percent = int((sent_count / total_emails) * 100)
        progress_bar.progress(percent)
        status_text.text(f"Đã đẩy batch {i//batch_size + 1} ({len(recipients_list)} emails) vào Kafka...")

    st.success(f"✅ Đã đẩy tổng cộng {sent_count} emails chia thành {math.ceil(total_emails/batch_size)} batches!")

def receive_reports(status_container):
    # 1. Khởi tạo đầy đủ các biến cần thiết
    if "metrics" not in st.session_state:
        st.session_state.metrics = {
            "total_success": 0, 
            "total_fail": 0, 
            "start_time": None,
            "end_time": None,
            "last_batch_time": None # Để tính tốc độ tức thời
        }
    # Dùng để vẽ "nhịp tim" tốc độ
    if "speed_chart_data" not in st.session_state:
        st.session_state.speed_chart_data = [] 
        
    if "batch_logs" not in st.session_state:
        st.session_state.batch_logs = []

    ws = connect_ws()
    if not ws: return

    while True:
        try:
            message = ws.recv()
            if message:
                data = json.loads(message)
                now = time.time()
                
                # --- LOGIC THỜI GIAN ---
                if st.session_state.metrics["start_time"] is None:
                    st.session_state.metrics["start_time"] = now
                
                st.session_state.metrics["end_time"] = now
                duration = now - st.session_state.metrics["start_time"]
                
                # Tính delta_t giữa 2 lần nhận batch để ra tốc độ tức thời
                last_t = st.session_state.metrics.get("last_batch_time") or st.session_state.metrics["start_time"]
                delta_t = now - last_t
                st.session_state.metrics["last_batch_time"] = now

                # --- CẬP NHẬT SỐ LIỆU ---
                st.session_state.metrics["total_success"] += data["success"]
                st.session_state.metrics["total_fail"] += data["fail"]
                
                # --- TÍNH TOÁN CHO BẢNG CHI TIẾT ---
                success_rate = (data["success"] / data["totalInBatch"]) * 100 if data["totalInBatch"] > 0 else 0
                instant_speed = data["totalInBatch"] / delta_t if delta_t > 0 else 0

                # LƯU DỮ LIỆU TỐC ĐỘ VÀO DANH SÁCH BIỂU ĐỒ
                # Chỉ giữ lại 50 điểm dữ liệu gần nhất để biểu đồ nhìn rõ "nhịp"
                st.session_state.speed_chart_data.append(instant_speed)
                if len(st.session_state.speed_chart_data) > 50:
                    st.session_state.speed_chart_data.pop(0)

                # Lưu log chi tiết cho dataframe
                st.session_state.batch_logs.append({
                    "STT": len(st.session_state.batch_logs) + 1,
                    "Thời gian": time.strftime("%H:%M:%S"),
                    "Chiến dịch": data.get("campaignId", "N/A")[:8],
                    "Thành công": data["success"],
                    "Thất bại": data["fail"],
                    "Tỉ lệ (%)": round(success_rate, 1),
                    "Tốc độ batch": f"{round(instant_speed, 1)} m/s",
                    "Tổng tích lũy": st.session_state.metrics["total_success"]
                })

                # --- HIỂN THỊ UI ---
                with status_container.container():
                    # 1. Metrics hàng đầu
                    m1, m2, m3, m4 = st.columns(4)
                    m1.metric("✅ Thành công", st.session_state.metrics["total_success"])
                    m2.metric("❌ Thất bại", st.session_state.metrics["total_fail"])
                    
                    avg_speed = st.session_state.metrics["total_success"] / duration if duration > 0 else 0
                    m3.metric("⚡ Tốc độ TB", f"{round(avg_speed, 1)} m/s")
                    m4.metric("⏱️ Tổng thời gian", f"{round(duration, 2)} s")

                    # 2. BIỂU ĐỒ "NHỊP TIM" TỐC ĐỘ
                    st.write("📊 **Nhịp tim hệ thống: Tốc độ xử lý Batch (m/s)**")
                    # Dùng line_chart để vẽ các điểm nhấp nhô
                    st.line_chart(st.session_state.speed_chart_data)

                    # 3. Bảng nhật ký vận hành
                    st.write("📄 **Nhật ký vận hành hệ thống:**")
                    # Sử dụng dataframe để có Progress Column
                    df_logs = st.session_state.batch_logs[::-1]
                    
                    st.dataframe(
                        df_logs,
                        column_config={
                            "Tỉ lệ (%)": st.column_config.ProgressColumn(
                                "Tỉ lệ thành công",
                                min_value=0, max_value=100, format="%d%%"
                            ),
                            "STT": st.column_config.TextColumn("ID"),
                            "Tốc độ batch": st.column_config.TextColumn("Tốc độ tức thời")
                        },
                        hide_index=True,
                        use_container_width=True
                    )

        except Exception as e:
            st.warning(f"Dừng nhận báo cáo: {e}")
            break
        
# --- 3. GIAO DIỆN CHÍNH ---

def main():
    st.set_page_config(page_title="Mail Monitor System", layout="wide")
    st.title("📧 Mail Batch Sender & Monitor")

    init_kafka_producer()

    col_control, col_monitor = st.columns([1, 2])

    with col_control:
        st.subheader("📤 Điều khiển Gửi")
        total = st.number_input("Tổng số mail", min_value=1, value=500)
        batch = 100
        
        if st.button("🚀 Bắt đầu tiến trình"):
            # Reset dữ liệu cũ để báo cáo mới chính xác
            st.session_state.batch_logs = []
            st.session_state.chart_data = []
            st.session_state.metrics = {"total_success": 0, "total_fail": 0, "start_time": None}

            # Khi nhấn nút, thực hiện kết nối WebSocket trước để 'giữ chỗ'
            ws = connect_ws()
            if ws:
                send_batch_to_kafka(total, batch)

    with col_monitor:
        st.subheader("🔔 Monitor (WebSocket)")
        # Kiểm tra xem có kết nối chưa để hiển thị trạng thái
        if "ws" in st.session_state and st.session_state.ws.connected:
            st.write("🟢 Trạng thái: Đã sẵn sàng nhận báo cáo")
            status_placeholder = st.empty()
            receive_reports(status_placeholder)
        else:
            st.info("⚪ Nhấn 'Bắt đầu' để kết nối và gửi mail.")

if __name__ == "__main__":
    main()