-- Create database
CREATE DATABASE "genai_db";

-- Connect to the new database and create tables
\c genai_db

CREATE TABLE IF NOT EXISTS urls (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL,
    s3_bucket           TEXT DEFAULT 'basebucket',
    s3_url              TEXT NOT NULL,
    presigned           BOOLEAN DEFAULT TRUE,
    method              VARCHAR(10) NOT NULL CHECK (method in ('GET', 'PUT')),
    expires_at          TIMESTAMP,
    created_at          TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS categories (
    id              INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR(32) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS category_items (
    id              INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR(32) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP,
    category_id     INTEGER,
    status          VARCHAR(16) DEFAULT 'PENDING' CHECK (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    CONSTRAINT fk_category_id
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS files (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    filename TEXT       NOT NULL UNIQUE,
    original_filename   TEXT,
    size_bytes BIGINT,
    file_creation_date  DATE,
    uploaded            BOOLEAN DEFAULT FALSE,
    uploaded_at         TIMESTAMP,
    category_item_id    INTEGER,

    CONSTRAINT fk_category_item_id
    FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

-- Generated from files, summary_generation worker input, and aggregate worker deletes them when it is finished
CREATE TABLE text_chunks (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL, -- Workers gets this from StartDTO
    chunk_index         INTEGER NOT NULL, -- Calculated by the worker (optional)
    page_start          INTEGER, -- Calculated by the worker (optional)
    page_end            INTEGER, -- Calculated by the worker (optional)
    text_content        TEXT NOT NULL, -- Calculated by the worker (optional)
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER, -- Workers gets this from StartDTO

    CONSTRAINT fk_chunks_file
        FOREIGN KEY (file_id) REFERENCES files(id),

    CONSTRAINT uq_chunks_file_index
        UNIQUE (file_id, chunk_index),
        
    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

-- Generated from text chunks, flashcard_generation worker input, and aggregate worker deletes them when it is finished
CREATE TABLE summary_chunks (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    chunk_id            INTEGER NOT NULL UNIQUE,
    summary_text        TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_summary_chunk -- Link back to the original text chunk (file, page, chunk_index info)
        FOREIGN KEY (chunk_id) REFERENCES text_chunks(id)
);

-- Generated from summary chunks, aggregate worker input and  deletes them when it is finished
CREATE TABLE temporary_flashcards (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    summary_chunk_id    INTEGER NOT NULL,
    question            TEXT NOT NULL,
    answer              TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_flashcard_summary -- Link back to the original summary chunk (file, page, chunk_index info)
        FOREIGN KEY (summary_chunk_id) REFERENCES summary_chunks(id)
);

-- Created from temporary flashcards by aggregate worker, linked to files and category items
CREATE TABLE final_flashcards (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL,
    question            TEXT NOT NULL,
    answer              TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_final_flashcard_file
        FOREIGN KEY (file_id) REFERENCES files(id),
        
    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

-- Created from summary chunks by aggregate worker, linked to files and category items
CREATE TABLE final_summaries (
    file_id             INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    summary_text        TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_final_summary_file
        FOREIGN KEY (file_id) REFERENCES files(id),
        
    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

CREATE TABLE jobs (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL,
    job_type            VARCHAR(30) CHECK (job_type in ('TEXT_EXTRACTION', 'SUMMARY_GENERATION', 'FLASHCARD_GENERATION', 'AGGREGATION')) NOT NULL,
    status              VARCHAR(30) CHECK (status in ('PENDING', 'IN_PROGRESS', 'FINISHED', 'FAILED')) NOT NULL,
    --total_tasks         INTEGER DEFAULT 0 NOT NULL,
    --completed_tasks     INTEGER DEFAULT 0 NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_jobs_file
        FOREIGN KEY (file_id) REFERENCES files(id),
        
    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

CREATE INDEX idx_chunks_file ON text_chunks(file_id);
CREATE INDEX idx_summary_file ON summary_chunks(file_id);
CREATE INDEX idx_flashcards_file ON flashcards(file_id);
CREATE INDEX idx_jobs_file ON jobs(file_id);
