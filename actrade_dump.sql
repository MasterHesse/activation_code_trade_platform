--
-- PostgreSQL database dump
--

\restrict FoStuN1bjHjjdd6gxl0wG1RoAu5flMnVAV1CfEfUuhXxMmrrP8MZBoisY4Xg2xY

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activation_code_inventory; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.activation_code_inventory (
    created_at timestamp(6) without time zone NOT NULL,
    expired_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    assigned_order_id uuid,
    assigned_order_item_id uuid,
    code_id uuid NOT NULL,
    merchant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    batch_no character varying(64) NOT NULL,
    code_value_hash character varying(128) NOT NULL,
    code_value_masked character varying(128) NOT NULL,
    code_value_encrypted text NOT NULL,
    remark character varying(255),
    CONSTRAINT activation_code_inventory_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'LOCKED'::character varying, 'SOLD'::character varying, 'VOID'::character varying])::text[])))
);


ALTER TABLE public.activation_code_inventory OWNER TO actrade;

--
-- Name: activation_tool; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.activation_tool (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    current_version_id uuid,
    merchant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    tool_id uuid NOT NULL,
    audit_status character varying(32) NOT NULL,
    tool_status character varying(32) NOT NULL,
    tool_name character varying(128) NOT NULL,
    CONSTRAINT activation_tool_audit_status_check CHECK (((audit_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT activation_tool_tool_status_check CHECK (((tool_status)::text = ANY ((ARRAY['ENABLED'::character varying, 'DISABLED'::character varying])::text[])))
);


ALTER TABLE public.activation_tool OWNER TO actrade;

--
-- Name: activation_tool_version; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.activation_tool_version (
    max_memory_mb integer,
    timeout_seconds integer,
    created_at timestamp(6) without time zone NOT NULL,
    file_size_bytes bigint,
    updated_at timestamp(6) without time zone NOT NULL,
    file_id uuid NOT NULL,
    tool_id uuid NOT NULL,
    tool_version_id uuid NOT NULL,
    runtime_arch character varying(32),
    runtime_os character varying(32),
    runtime_type character varying(32),
    scan_status character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    checksum_sha256 character varying(64),
    version_name character varying(64) NOT NULL,
    entrypoint character varying(255),
    exec_command text,
    review_remark text,
    scan_report text,
    manifest_content jsonb,
    CONSTRAINT activation_tool_version_runtime_arch_check CHECK (((runtime_arch)::text = ANY ((ARRAY['AMD64'::character varying, 'ARM64'::character varying])::text[]))),
    CONSTRAINT activation_tool_version_runtime_os_check CHECK (((runtime_os)::text = ANY ((ARRAY['LINUX'::character varying, 'WINDOWS'::character varying])::text[]))),
    CONSTRAINT activation_tool_version_runtime_type_check CHECK (((runtime_type)::text = ANY ((ARRAY['JAR'::character varying, 'SHELL'::character varying, 'NATIVE_BINARY'::character varying])::text[]))),
    CONSTRAINT activation_tool_version_scan_status_check CHECK (((scan_status)::text = ANY ((ARRAY['PENDING'::character varying, 'SAFE'::character varying, 'DANGEROUS'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT activation_tool_version_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING'::character varying, 'ENABLED'::character varying, 'DISABLED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.activation_tool_version OWNER TO actrade;

--
-- Name: app_users; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.app_users (
    created_at timestamp(6) without time zone NOT NULL,
    last_login_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    user_id uuid NOT NULL,
    phone character varying(32),
    role character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    nickname character varying(64),
    username character varying(64) NOT NULL,
    email character varying(128),
    password_hash character varying(255) NOT NULL,
    CONSTRAINT app_users_role_check CHECK (((role)::text = ANY ((ARRAY['ROLE_USER'::character varying, 'ROLE_MERCHANT'::character varying, 'ROLE_ADMIN'::character varying, 'ROLE_SUPER_ADMIN'::character varying])::text[]))),
    CONSTRAINT app_users_status_check CHECK (((status)::text = ANY ((ARRAY['NORMAL'::character varying, 'DISABLED'::character varying, 'DELETED'::character varying])::text[])))
);


ALTER TABLE public.app_users OWNER TO actrade;

--
-- Name: file_asset; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.file_asset (
    created_at timestamp(6) without time zone NOT NULL,
    file_size_bytes bigint NOT NULL,
    file_id uuid NOT NULL,
    uploaded_by uuid NOT NULL,
    scan_status character varying(32) NOT NULL,
    storage_provider character varying(32) NOT NULL,
    checksum_sha256 character varying(64) NOT NULL,
    bucket_name character varying(128) NOT NULL,
    content_type character varying(128) NOT NULL,
    object_key character varying(255) NOT NULL,
    original_filename character varying(255) NOT NULL,
    scan_report text,
    stored_filename character varying(255) NOT NULL,
    CONSTRAINT file_asset_scan_status_check CHECK (((scan_status)::text = ANY ((ARRAY['PENDING'::character varying, 'SAFE'::character varying, 'DANGEROUS'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT file_asset_storage_provider_check CHECK (((storage_provider)::text = ANY ((ARRAY['MINIO'::character varying, 'S3'::character varying, 'LOCAL'::character varying])::text[])))
);


ALTER TABLE public.file_asset OWNER TO actrade;

--
-- Name: merchant; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.merchant (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    merchant_id uuid NOT NULL,
    user_id uuid NOT NULL,
    audit_status character varying(32) NOT NULL,
    contact_phone character varying(32),
    merchant_type character varying(32) NOT NULL,
    contact_name character varying(64),
    contact_email character varying(128),
    merchant_name character varying(128) NOT NULL,
    audit_remark text,
    license_info text,
    CONSTRAINT merchant_audit_status_check CHECK (((audit_status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT merchant_merchant_type_check CHECK (((merchant_type)::text = ANY ((ARRAY['PERSONAL'::character varying, 'TEAM'::character varying, 'ENTERPRISE'::character varying])::text[])))
);


ALTER TABLE public.merchant OWNER TO actrade;

--
-- Name: order_items; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.order_items (
    quantity integer NOT NULL,
    subtotal_amount numeric(18,2) NOT NULL,
    unit_price numeric(18,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    order_id uuid NOT NULL,
    order_item_id uuid NOT NULL,
    product_id uuid NOT NULL,
    delivery_mode character varying(32) NOT NULL,
    product_name character varying(255) NOT NULL,
    CONSTRAINT order_items_delivery_mode_check CHECK (((delivery_mode)::text = ANY ((ARRAY['CODE_STOCK'::character varying, 'TOOL_EXECUTION'::character varying])::text[])))
);


ALTER TABLE public.order_items OWNER TO actrade;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.orders (
    pay_amount numeric(18,2) NOT NULL,
    total_amount numeric(18,2) NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    paid_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    merchant_id uuid NOT NULL,
    order_id uuid NOT NULL,
    user_id uuid NOT NULL,
    order_status character varying(32) NOT NULL,
    payment_method character varying(32),
    payment_status character varying(32) NOT NULL,
    channel_trade_no character varying(64),
    order_no character varying(64) NOT NULL,
    payment_request_no character varying(64),
    remark character varying(500),
    closed_at timestamp(6) without time zone,
    confirm_deadline_at timestamp(6) without time zone,
    confirmed_at timestamp(6) without time zone,
    delivered_at timestamp(6) without time zone,
    fulfillment_status character varying(32),
    fulfillment_type character varying(32),
    pay_deadline_at timestamp(6) without time zone,
    platform_income_amount numeric(18,2),
    seller_id uuid,
    seller_income_amount numeric(18,2),
    settlement_status character varying(32),
    CONSTRAINT orders_fulfillment_status_check CHECK (((fulfillment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT orders_fulfillment_type_check CHECK (((fulfillment_type)::text = ANY ((ARRAY['ACTIVATION_TOOL'::character varying, 'STOCK_CODE'::character varying])::text[]))),
    CONSTRAINT orders_order_status_check CHECK (((order_status)::text = ANY ((ARRAY['CREATED'::character varying, 'PAID'::character varying, 'DELIVERING'::character varying, 'COMPLETED'::character varying, 'DELIVERY_FAILED'::character varying, 'CANCELED'::character varying])::text[]))),
    CONSTRAINT orders_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['MOCK'::character varying, 'MANUAL'::character varying, 'ALIPAY'::character varying])::text[]))),
    CONSTRAINT orders_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['UNPAID'::character varying, 'PAYING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'CLOSED'::character varying, 'REFUNDED'::character varying])::text[]))),
    CONSTRAINT orders_settlement_status_check CHECK (((settlement_status)::text = ANY ((ARRAY['UNSETTLED'::character varying, 'PENDING'::character varying, 'SETTLED'::character varying])::text[])))
);


ALTER TABLE public.orders OWNER TO actrade;

--
-- Name: product; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.product (
    original_price numeric(12,2),
    price numeric(12,2) NOT NULL,
    sales_count integer NOT NULL,
    stock_count integer NOT NULL,
    version_no integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    category_id uuid NOT NULL,
    merchant_id uuid NOT NULL,
    product_id uuid NOT NULL,
    delivery_mode character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    name character varying(128) NOT NULL,
    cover_image character varying(255),
    description text,
    subtitle character varying(255),
    CONSTRAINT product_delivery_mode_check CHECK (((delivery_mode)::text = ANY ((ARRAY['CODE_STOCK'::character varying, 'TOOL_EXECUTION'::character varying])::text[]))),
    CONSTRAINT product_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_REVIEW'::character varying, 'ONLINE'::character varying, 'OFFLINE'::character varying])::text[])))
);


ALTER TABLE public.product OWNER TO actrade;

--
-- Name: product_category; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.product_category (
    sort_no integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    category_id uuid NOT NULL,
    parent_id uuid,
    status character varying(32) NOT NULL,
    name character varying(64) NOT NULL,
    CONSTRAINT product_category_status_check CHECK (((status)::text = ANY ((ARRAY['ENABLED'::character varying, 'DISABLED'::character varying])::text[])))
);


ALTER TABLE public.product_category OWNER TO actrade;

--
-- Name: product_detail; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.product_detail (
    updated_at timestamp(6) without time zone NOT NULL,
    product_id uuid NOT NULL,
    activation_notice text,
    detail_markdown text,
    faq_content text,
    refund_policy text,
    usage_guide text
);


ALTER TABLE public.product_detail OWNER TO actrade;

--
-- Name: seller_settlements; Type: TABLE; Schema: public; Owner: actrade
--

CREATE TABLE public.seller_settlements (
    settlement_id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    merchant_id uuid NOT NULL,
    order_id uuid NOT NULL,
    remark character varying(500),
    seller_id uuid NOT NULL,
    settled_at timestamp(6) without time zone,
    settlement_amount numeric(18,2) NOT NULL,
    settlement_no character varying(64) NOT NULL,
    settlement_status character varying(32) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    version bigint NOT NULL,
    CONSTRAINT seller_settlements_settlement_status_check CHECK (((settlement_status)::text = ANY ((ARRAY['UNSETTLED'::character varying, 'PENDING'::character varying, 'SETTLED'::character varying])::text[])))
);


ALTER TABLE public.seller_settlements OWNER TO actrade;

--
-- Data for Name: activation_code_inventory; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.activation_code_inventory (created_at, expired_at, updated_at, assigned_order_id, assigned_order_item_id, code_id, merchant_id, product_id, status, batch_no, code_value_hash, code_value_masked, code_value_encrypted, remark) FROM stdin;
\.


--
-- Data for Name: activation_tool; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.activation_tool (created_at, updated_at, current_version_id, merchant_id, product_id, tool_id, audit_status, tool_status, tool_name) FROM stdin;
\.


--
-- Data for Name: activation_tool_version; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.activation_tool_version (max_memory_mb, timeout_seconds, created_at, file_size_bytes, updated_at, file_id, tool_id, tool_version_id, runtime_arch, runtime_os, runtime_type, scan_status, status, checksum_sha256, version_name, entrypoint, exec_command, review_remark, scan_report, manifest_content) FROM stdin;
\.


--
-- Data for Name: app_users; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.app_users (created_at, last_login_at, updated_at, user_id, phone, role, status, nickname, username, email, password_hash) FROM stdin;
2026-03-16 13:04:01.282542	\N	2026-03-16 13:04:01.282542	3f7adfce-b1c9-4dec-abe5-264ea3118a17	13900139000	ROLE_USER	NORMAL	测试买家用户	testbuyer_1773637440	testbuyr@example.com	$2a$10$L.AIhaoYCSISDz7kVXKNlOBTX3abbVBlshAGGaw1bdUWreGYP0xnC
2026-03-16 13:04:51.685699	\N	2026-03-16 13:04:51.685699	d4977595-b87f-45e3-a9a0-af4f17e7d697	13800138000	ROLE_USER	NORMAL	测试用户	testuser_1773637491	testuser@example.com	$2a$10$tS6IRozkPS9kmJ4abIEHw.xldWWXivYSsksHTgdM1Yg11VOjCJwMS
\.


--
-- Data for Name: file_asset; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.file_asset (created_at, file_size_bytes, file_id, uploaded_by, scan_status, storage_provider, checksum_sha256, bucket_name, content_type, object_key, original_filename, scan_report, stored_filename) FROM stdin;
\.


--
-- Data for Name: merchant; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.merchant (created_at, updated_at, merchant_id, user_id, audit_status, contact_phone, merchant_type, contact_name, contact_email, merchant_name, audit_remark, license_info) FROM stdin;
2026-03-16 13:05:08.473397	2026-03-16 13:05:08.473442	04e0bc3f-0b9e-4d33-bfa0-b973660cbc2c	d4977595-b87f-45e3-a9a0-af4f17e7d697	PENDING	13900139000	TEAM	联系人	contact@merchant.com	测试商家	\N	LICENSE123
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.order_items (quantity, subtotal_amount, unit_price, created_at, order_id, order_item_id, product_id, delivery_mode, product_name) FROM stdin;
5	499.95	99.99	2026-03-16 13:09:09.294986	e161eecf-09b6-4a90-ab0e-5132582a994a	b6bb8be0-9010-4764-b800-640fad680f7a	d5e761f4-ba0a-4f34-bbf2-0609e85c6919	TOOL_EXECUTION	工具执行商品
6	1199.94	199.99	2026-03-16 13:09:09.295295	e161eecf-09b6-4a90-ab0e-5132582a994a	25cd7ab2-6ef3-4f22-9ef8-a2c2909c8fd1	817c6c74-51d0-4c6d-9515-dd0fd87a7a57	CODE_STOCK	代码库存商品
3	299.97	99.99	2026-03-16 13:31:12.925917	05caf5b5-9ba7-46f3-8a6b-7d66a45258f4	fb0c6ac7-ed06-4f21-8e52-a502f5b8d5e0	d5e761f4-ba0a-4f34-bbf2-0609e85c6919	TOOL_EXECUTION	工具执行商品
7	1399.93	199.99	2026-03-16 13:31:12.926172	05caf5b5-9ba7-46f3-8a6b-7d66a45258f4	78515f72-b6cb-4fb1-b9f5-0ed40799a918	817c6c74-51d0-4c6d-9515-dd0fd87a7a57	CODE_STOCK	代码库存商品
10	999.90	99.99	2026-03-16 13:41:12.008854	240962aa-fcdd-4567-a82c-a51a3ce09049	495df749-18a7-4919-abd2-b3dbc64b10cd	d5e761f4-ba0a-4f34-bbf2-0609e85c6919	TOOL_EXECUTION	工具执行商品
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.orders (pay_amount, total_amount, created_at, paid_at, updated_at, version, merchant_id, order_id, user_id, order_status, payment_method, payment_status, channel_trade_no, order_no, payment_request_no, remark, closed_at, confirm_deadline_at, confirmed_at, delivered_at, fulfillment_status, fulfillment_type, pay_deadline_at, platform_income_amount, seller_id, seller_income_amount, settlement_status) FROM stdin;
1699.89	1699.89	2026-03-16 13:09:09.293584	\N	2026-03-16 13:10:58.249424	2	04e0bc3f-0b9e-4d33-bfa0-b973660cbc2c	e161eecf-09b6-4a90-ab0e-5132582a994a	3f7adfce-b1c9-4dec-abe5-264ea3118a17	CREATED	ALIPAY	PAYING	\N	ORD20260316130909538618	PAY20260316131058233298	Order测试专用	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
1699.90	1699.90	2026-03-16 13:31:12.919404	\N	2026-03-16 13:34:16.850801	2	04e0bc3f-0b9e-4d33-bfa0-b973660cbc2c	05caf5b5-9ba7-46f3-8a6b-7d66a45258f4	3f7adfce-b1c9-4dec-abe5-264ea3118a17	CREATED	ALIPAY	PAYING	\N	ORD20260316133112673365	PAY20260316133416684484	Order测试专用-2（修复版）	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
999.90	999.90	2026-03-16 13:41:12.004045	2026-03-16 13:47:58.729126	2026-03-16 13:47:58.729657	3	04e0bc3f-0b9e-4d33-bfa0-b973660cbc2c	240962aa-fcdd-4567-a82c-a51a3ce09049	3f7adfce-b1c9-4dec-abe5-264ea3118a17	PAID	ALIPAY	PAID	2026031622001443880509253082	ORD20260316134111090035	PAY20260316134218769038	Order测试专用3	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
\.


--
-- Data for Name: product; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.product (original_price, price, sales_count, stock_count, version_no, created_at, updated_at, category_id, merchant_id, product_id, delivery_mode, status, name, cover_image, description, subtitle) FROM stdin;
129.99	99.99	0	100	0	2026-03-16 13:06:43.298098	2026-03-16 13:06:43.298122	9553b32b-3acc-47f7-be44-26469b5f7f35	04e0bc3f-0b9e-4d33-bfa0-b973660cbc2c	d5e761f4-ba0a-4f34-bbf2-0609e85c6919	TOOL_EXECUTION	ONLINE	工具执行商品	http://example.com/tool.jpg	这是一个工具执行模式的商品	用于工具执行测试
229.99	199.99	0	200	0	2026-03-16 13:07:04.739122	2026-03-16 13:07:04.739191	9553b32b-3acc-47f7-be44-26469b5f7f35	04e0bc3f-0b9e-4d33-bfa0-b973660cbc2c	817c6c74-51d0-4c6d-9515-dd0fd87a7a57	CODE_STOCK	ONLINE	代码库存商品	http://example.com/code.jpg	这是一个代码库存模式的商品	用于代码库存测试
\.


--
-- Data for Name: product_category; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.product_category (sort_no, created_at, category_id, parent_id, status, name) FROM stdin;
1	2026-03-16 13:06:10.106263	9553b32b-3acc-47f7-be44-26469b5f7f35	\N	ENABLED	测试分类
\.


--
-- Data for Name: product_detail; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.product_detail (updated_at, product_id, activation_notice, detail_markdown, faq_content, refund_policy, usage_guide) FROM stdin;
\.


--
-- Data for Name: seller_settlements; Type: TABLE DATA; Schema: public; Owner: actrade
--

COPY public.seller_settlements (settlement_id, created_at, merchant_id, order_id, remark, seller_id, settled_at, settlement_amount, settlement_no, settlement_status, updated_at, version) FROM stdin;
\.


--
-- Name: activation_code_inventory activation_code_inventory_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_code_inventory
    ADD CONSTRAINT activation_code_inventory_pkey PRIMARY KEY (code_id);


--
-- Name: activation_tool activation_tool_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool
    ADD CONSTRAINT activation_tool_pkey PRIMARY KEY (tool_id);


--
-- Name: activation_tool_version activation_tool_version_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool_version
    ADD CONSTRAINT activation_tool_version_pkey PRIMARY KEY (tool_version_id);


--
-- Name: app_users app_users_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (user_id);


--
-- Name: file_asset file_asset_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.file_asset
    ADD CONSTRAINT file_asset_pkey PRIMARY KEY (file_id);


--
-- Name: merchant merchant_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.merchant
    ADD CONSTRAINT merchant_pkey PRIMARY KEY (merchant_id);


--
-- Name: merchant merchant_user_id_key; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.merchant
    ADD CONSTRAINT merchant_user_id_key UNIQUE (user_id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (order_item_id);


--
-- Name: orders orders_payment_request_no_key; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_payment_request_no_key UNIQUE (payment_request_no);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (order_id);


--
-- Name: product_category product_category_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT product_category_pkey PRIMARY KEY (category_id);


--
-- Name: product_detail product_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.product_detail
    ADD CONSTRAINT product_detail_pkey PRIMARY KEY (product_id);


--
-- Name: product product_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (product_id);


--
-- Name: seller_settlements seller_settlements_pkey; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.seller_settlements
    ADD CONSTRAINT seller_settlements_pkey PRIMARY KEY (settlement_id);


--
-- Name: activation_tool uk_activation_tool_product_id; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool
    ADD CONSTRAINT uk_activation_tool_product_id UNIQUE (product_id);


--
-- Name: app_users uk_app_users_email; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk_app_users_email UNIQUE (email);


--
-- Name: app_users uk_app_users_phone; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk_app_users_phone UNIQUE (phone);


--
-- Name: app_users uk_app_users_username; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk_app_users_username UNIQUE (username);


--
-- Name: activation_code_inventory uk_code_inventory_product_code_hash; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_code_inventory
    ADD CONSTRAINT uk_code_inventory_product_code_hash UNIQUE (product_id, code_value_hash);


--
-- Name: merchant uk_merchant_user_id; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.merchant
    ADD CONSTRAINT uk_merchant_user_id UNIQUE (user_id);


--
-- Name: orders uk_orders_order_no; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT uk_orders_order_no UNIQUE (order_no);


--
-- Name: seller_settlements uk_seller_settlements_order_id; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.seller_settlements
    ADD CONSTRAINT uk_seller_settlements_order_id UNIQUE (order_id);


--
-- Name: seller_settlements uk_seller_settlements_settlement_no; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.seller_settlements
    ADD CONSTRAINT uk_seller_settlements_settlement_no UNIQUE (settlement_no);


--
-- Name: activation_tool_version uk_tool_version_tool_id_version_name; Type: CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool_version
    ADD CONSTRAINT uk_tool_version_tool_id_version_name UNIQUE (tool_id, version_name);


--
-- Name: idx_activation_tool_current_version_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_activation_tool_current_version_id ON public.activation_tool USING btree (current_version_id);


--
-- Name: idx_activation_tool_merchant_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_activation_tool_merchant_status ON public.activation_tool USING btree (merchant_id, tool_status);


--
-- Name: idx_code_inventory_assigned_order_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_code_inventory_assigned_order_id ON public.activation_code_inventory USING btree (assigned_order_id);


--
-- Name: idx_code_inventory_batch_no; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_code_inventory_batch_no ON public.activation_code_inventory USING btree (batch_no);


--
-- Name: idx_code_inventory_expired_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_code_inventory_expired_at ON public.activation_code_inventory USING btree (expired_at);


--
-- Name: idx_code_inventory_product_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_code_inventory_product_status ON public.activation_code_inventory USING btree (product_id, status);


--
-- Name: idx_file_asset_bucket_object_key; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_file_asset_bucket_object_key ON public.file_asset USING btree (bucket_name, object_key);


--
-- Name: idx_file_asset_checksum_sha256; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_file_asset_checksum_sha256 ON public.file_asset USING btree (checksum_sha256);


--
-- Name: idx_file_asset_uploaded_by; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_file_asset_uploaded_by ON public.file_asset USING btree (uploaded_by);


--
-- Name: idx_merchant_audit_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_merchant_audit_status ON public.merchant USING btree (audit_status);


--
-- Name: idx_merchant_created_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_merchant_created_at ON public.merchant USING btree (created_at);


--
-- Name: idx_order_items_created_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_order_items_created_at ON public.order_items USING btree (created_at);


--
-- Name: idx_order_items_order_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_order_items_order_id ON public.order_items USING btree (order_id);


--
-- Name: idx_order_items_product_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_order_items_product_id ON public.order_items USING btree (product_id);


--
-- Name: idx_orders_confirm_deadline_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_confirm_deadline_at ON public.orders USING btree (confirm_deadline_at);


--
-- Name: idx_orders_created_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_created_at ON public.orders USING btree (created_at);


--
-- Name: idx_orders_merchant_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_merchant_id ON public.orders USING btree (merchant_id);


--
-- Name: idx_orders_order_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_order_status ON public.orders USING btree (order_status);


--
-- Name: idx_orders_pay_deadline_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_pay_deadline_at ON public.orders USING btree (pay_deadline_at);


--
-- Name: idx_orders_payment_request_no; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_payment_request_no ON public.orders USING btree (payment_request_no);


--
-- Name: idx_orders_payment_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_payment_status ON public.orders USING btree (payment_status);


--
-- Name: idx_orders_seller_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_seller_id ON public.orders USING btree (seller_id);


--
-- Name: idx_orders_user_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_orders_user_id ON public.orders USING btree (user_id);


--
-- Name: idx_product_category_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_category_id ON public.product USING btree (category_id);


--
-- Name: idx_product_category_parent_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_category_parent_id ON public.product_category USING btree (parent_id);


--
-- Name: idx_product_category_sort_no; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_category_sort_no ON public.product_category USING btree (sort_no);


--
-- Name: idx_product_category_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_category_status ON public.product_category USING btree (status);


--
-- Name: idx_product_created_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_created_at ON public.product USING btree (created_at);


--
-- Name: idx_product_delivery_mode; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_delivery_mode ON public.product USING btree (delivery_mode);


--
-- Name: idx_product_merchant_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_merchant_id ON public.product USING btree (merchant_id);


--
-- Name: idx_product_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_product_status ON public.product USING btree (status);


--
-- Name: idx_seller_settlements_created_at; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_seller_settlements_created_at ON public.seller_settlements USING btree (created_at);


--
-- Name: idx_seller_settlements_merchant_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_seller_settlements_merchant_id ON public.seller_settlements USING btree (merchant_id);


--
-- Name: idx_seller_settlements_order_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_seller_settlements_order_id ON public.seller_settlements USING btree (order_id);


--
-- Name: idx_seller_settlements_seller_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_seller_settlements_seller_id ON public.seller_settlements USING btree (seller_id);


--
-- Name: idx_seller_settlements_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_seller_settlements_status ON public.seller_settlements USING btree (settlement_status);


--
-- Name: idx_tool_version_file_id; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_tool_version_file_id ON public.activation_tool_version USING btree (file_id);


--
-- Name: idx_tool_version_tool_id_status; Type: INDEX; Schema: public; Owner: actrade
--

CREATE INDEX idx_tool_version_tool_id_status ON public.activation_tool_version USING btree (tool_id, status);


--
-- Name: activation_tool fk_activation_tool_current_version; Type: FK CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool
    ADD CONSTRAINT fk_activation_tool_current_version FOREIGN KEY (current_version_id) REFERENCES public.activation_tool_version(tool_version_id);


--
-- Name: activation_tool_version fk_activation_tool_version_file; Type: FK CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool_version
    ADD CONSTRAINT fk_activation_tool_version_file FOREIGN KEY (file_id) REFERENCES public.file_asset(file_id);


--
-- Name: activation_tool_version fk_activation_tool_version_tool; Type: FK CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.activation_tool_version
    ADD CONSTRAINT fk_activation_tool_version_tool FOREIGN KEY (tool_id) REFERENCES public.activation_tool(tool_id);


--
-- Name: product_detail fk_product_detail_product; Type: FK CONSTRAINT; Schema: public; Owner: actrade
--

ALTER TABLE ONLY public.product_detail
    ADD CONSTRAINT fk_product_detail_product FOREIGN KEY (product_id) REFERENCES public.product(product_id);


--
-- PostgreSQL database dump complete
--

\unrestrict FoStuN1bjHjjdd6gxl0wG1RoAu5flMnVAV1CfEfUuhXxMmrrP8MZBoisY4Xg2xY

