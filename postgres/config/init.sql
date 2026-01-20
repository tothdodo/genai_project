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
    id                     INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name                   VARCHAR(32) NOT NULL,
    description            TEXT,
    created_at             TIMESTAMP,
    category_id            INTEGER,
    status                 VARCHAR(16) DEFAULT 'PENDING' CHECK (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    failed_job_type        VARCHAR(30) CHECK (failed_job_type in ('TEXT_EXTRACTION', 'SUMMARY_GENERATION', 'FLASHCARD_GENERATION', 'AGGREGATION')),

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
    url                 TEXT,

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
    text_chunk_id            INTEGER NOT NULL UNIQUE,
    summary_text        TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_summary_chunk -- Link back to the original text chunk (file, page, chunk_index info)
        FOREIGN KEY (text_chunk_id) REFERENCES text_chunks(id)
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
    question            TEXT NOT NULL,
    answer              TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

-- Created from summary chunks by aggregate worker, linked to files and category items
CREATE TABLE final_summaries (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    summary_text        TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER UNIQUE,

    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

CREATE TABLE jobs (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER,
    job_type            VARCHAR(30) CHECK (job_type in ('TEXT_EXTRACTION', 'SUMMARY_GENERATION', 'FLASHCARD_GENERATION', 'AGGREGATION')) NOT NULL,
    status              VARCHAR(30) CHECK (status in ('PENDING', 'IN_PROGRESS', 'FINISHED', 'FAILED', 'CANCELLED')) NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_jobs_file
        FOREIGN KEY (file_id) REFERENCES files(id),
        
    CONSTRAINT fk_category_item_id
        FOREIGN KEY (category_item_id) REFERENCES category_items(id) ON DELETE SET NULL
);

-- To Delete Text Chunks/Summary Chunks/Temp Flashcards efficiently by category_item_id when generation is finished
CREATE INDEX idx_chunks_category_item ON text_chunks(category_item_id);
CREATE INDEX idx_summary_text_chunk ON summary_chunks(text_chunk_id);
CREATE INDEX idx_temp_flashcards_summary_chunk ON temporary_flashcards(summary_chunk_id);

-- To Get Final Flashcards/Summaries by category_item_id efficiently
CREATE INDEX idx_final_flashcards_category_item ON final_flashcards(category_item_id);
CREATE INDEX idx_final_summaries_category_item ON final_summaries(category_item_id);

-- To Get Jobs by file_id and status/job_type efficiently
CREATE INDEX idx_jobs_file_id ON jobs(file_id);
CREATE INDEX idx_jobs_status_type ON jobs(status, job_type);
