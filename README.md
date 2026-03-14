# ACTrade

> A modern activation-code / digital-goods trading platform built with Spring Boot and PostgreSQL.

ACTrade 是一个面向 **激活码、卡密、数字商品与工具型商品** 的交易平台项目。  
项目当前以 **后端 API 能力建设** 为主，已完成用户、商家、商品、商品分类、商品详情等核心基础模块的搭建与联调。

---

## ✨ Features

- 用户模块基础能力
- 商家模块基础能力
- 商品分类管理
- 商品基础信息管理
- 商品详情维护
- 通用 API 响应封装
- 全局异常处理
- PostgreSQL 数据持久化
- Docker Compose 本地基础设施启动

---

## 🧱 Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3.5.11**
- **Spring Data JPA / Hibernate**
- **PostgreSQL**
- **Maven**

### Infra
- **Docker Compose**

### Planned
- Frontend (`actrade-web`)
- Authentication & Authorization
- Order System
- Inventory / Activation Code Management
- Payment Integration

---

## 📁 Project Structure

```text
.
├── actrade-api        # Spring Boot backend service
├── actrade-web        # Frontend project (planned / reserved)
├── documents          # Project documents
├── infra              # Infrastructure config (docker-compose)
└── README.md
```

### Backend package design

```text
com.masterhesse
├── app_users
├── merchant
├── product
├── common
├── config
└── web
```

Each business module follows a layered structure:

- `api` — Controller / request-response DTO
- `application` — Application services
- `domain` — Entities / enums / core models
- `persistence` — Repository / persistence layer

---

## 🚀 Quick Start

### 1. Start infrastructure

```bash
cd infra
docker compose up -d
```

### 2. Start backend service

```bash
cd actrade-api
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

---

## 🗄️ Current Data Model

The project currently includes these core tables:

- `app_users`
- `merchant`
- `product_category`
- `product`
- `product_detail`

Among them:

- `merchant.license_info` uses `text`
- `merchant.audit_remark` uses `text`
- `product.description` uses `text`
- `product_detail.*` long-text fields use `text`

This design is intended to better support PostgreSQL long-text storage scenarios.

---

## ✅ Current Progress

- [x] Project base structure established
- [x] User module basic structure
- [x] Merchant module CRUD foundation
- [x] Product category module foundation
- [x] Product / product detail module foundation
- [x] Global exception handling
- [x] Unified API response wrapper
- [x] PostgreSQL local initialization verified
- [x] Merchant and product test data initialization passed

---

## 🛣️ Roadmap

- [ ] Login / authentication system
- [ ] Role-based permission control
- [ ] Order module
- [ ] Card / activation code inventory management
- [ ] Delivery workflow abstraction
- [ ] Payment integration
- [ ] Admin console
- [ ] Frontend application bootstrap
- [ ] API documentation
- [ ] Unit and integration tests improvement

---

## 💡 Project Vision

ACTrade aims to provide a clean and extensible foundation for building a digital-goods trading platform, especially for scenarios such as:

- activation codes
- software licenses
- card-secret inventory
- automated tool delivery
- lightweight digital commerce systems

The project is currently in an early iteration stage, with emphasis on **clear structure**, **stable data modeling**, and **maintainable backend architecture**.

---

## 📚 Documents

Project documents are stored in the `documents` directory, including:

- project planning notes
- structure documentation
- future design drafts

---

## 🤝 Contributing

Issues and suggestions are welcome.  
If you find a bug or have an idea for improvement, feel free to open an issue or submit a pull request.

---

## 📄 License

This project is currently intended for learning, experimentation, and architecture practice.  
License can be added later as the project evolves.
