# 🚀 DineSphere - Backend

## 📌 Overview
This is the backend service for a QR-based ordering system designed for restaurants and hotels.  
It enables customers to scan a QR code and place orders seamlessly without manual interaction.

The system supports:
- Order management
- Billing & finance tracking
- Room & table-based ordering
- Live tracking & notifications
- Analytics & reporting

---

## 🧱 Tech Stack
- Java (Spring Boot)
- MongoDB
- Redis (caching)
- REST APIs
- JWT (Authentication)
- OkHttp (External API calls)
- Docker (optional)

---

## ⚙️ Features

### 🛒 Order Management
- Create, update, track orders
- Separate flows for restaurant & hotel rooms

### 💳 Billing & Finance
- Auto bill generation
- Payment tracking
- Revenue insights

### 🏨 Room / Table System
- QR mapped to table/room
- Session-based ordering

### 📊 Analytics
- Order trends
- Revenue reports
- Customer insights

### 🔔 Notifications
- Real-time order updates
- Admin alerts

### 📍 Live Tracking
- Order status tracking (Placed → Preparing → Delivered)

---

## 🔐 Authentication
- JWT-based authentication
- Session binding (IP + User-Agent)
- Secure QR token validation

---

## 📁 Project Structure
