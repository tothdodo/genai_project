-- Create database
CREATE DATABASE "genai_db";

-- Connect to the new database and create tables
\c genai_db

CREATE TABLE IF NOT EXISTS urls (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id INTEGER NOT NULL,
    s3_bucket TEXT DEFAULT 'basebucket',
    s3_url TEXT NOT NULL,
    presigned BOOLEAN DEFAULT TRUE,
    method VARCHAR(10) NOT NULL CHECK (method in ('GET', 'PUT')),
    expires_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(32) NOT NULL,
    description TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category_items (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(32) NOT NULL,
    description TEXT,
    created_at TIMESTAMP,
    category_id INTEGER,
    CONSTRAINT fk_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE 
);

CREATE TABLE IF NOT EXISTS files (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    filename TEXT NOT NULL UNIQUE,
    original_filename TEXT,
    size_bytes BIGINT,
    file_creation_date DATE,
    --status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' CHECK (status in ('UPLOADED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    uploaded BOOLEAN DEFAULT FALSE,
    uploaded_at TIMESTAMP,
    category_item_id INTEGER,
    CONSTRAINT fk_category_item_id FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);