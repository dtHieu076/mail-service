# ========================
# Base image Python
# ========================
FROM python:3.11-slim

# ========================
# Environment
# ========================
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# ========================
# Workdir
# ========================
WORKDIR /app

# ========================
# Copy & install dependencies
# ========================
COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

# ========================
# Copy source code
# ========================
COPY . .

# ========================
# Expose Streamlit port
# ========================
EXPOSE 8501

# ========================
# Run Streamlit
# ========================
CMD ["streamlit", "run", "app.py", "--server.port=8501", "--server.address=0.0.0.0"]