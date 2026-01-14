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

CREATE TABLE text_chunks (
     id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
     file_id             INTEGER NOT NULL,
     chunk_index         INTEGER NOT NULL,
     page_start          INTEGER,
     page_end            INTEGER,
     text_content        TEXT NOT NULL,
     created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     category_item_id    INTEGER,

     CONSTRAINT fk_chunks_file
         FOREIGN KEY (file_id) REFERENCES files(id),

     CONSTRAINT uq_chunks_file_index
         UNIQUE (file_id, chunk_index)
);

CREATE TABLE summary_chunks (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL,
    chunk_id            INTEGER NOT NULL,
    summary_text        TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_summary_file
        FOREIGN KEY (file_id) REFERENCES files(id),

    CONSTRAINT fk_summary_chunk
        FOREIGN KEY (chunk_id) REFERENCES text_chunks(id),

    CONSTRAINT uq_summary_chunk
        UNIQUE (chunk_id)
);

-- Generated from summary chunks, not raw text.
CREATE TABLE flashcards (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL,
    summary_chunk_id    INTEGER NOT NULL,
    question            TEXT NOT NULL,
    answer              TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_flashcard_file
        FOREIGN KEY (file_id) REFERENCES files(id),

    CONSTRAINT fk_flashcard_summary
        FOREIGN KEY (summary_chunk_id) REFERENCES summary_chunks(id)
);

CREATE TABLE final_summaries (
    file_id             INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    summary_text        TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_final_summary_file
     FOREIGN KEY (file_id) REFERENCES files(id)
);

CREATE TABLE jobs (
    id                  INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    file_id             INTEGER NOT NULL,
    job_type            VARCHAR(50) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    total_tasks         INTEGER DEFAULT 0 NOT NULL,
    completed_tasks     INTEGER DEFAULT 0 NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category_item_id    INTEGER,

    CONSTRAINT fk_jobs_file
      FOREIGN KEY (file_id) REFERENCES files(id)
);

CREATE INDEX idx_chunks_file ON text_chunks(file_id);
CREATE INDEX idx_summary_file ON summary_chunks(file_id);
CREATE INDEX idx_flashcards_file ON flashcards(file_id);
CREATE INDEX idx_jobs_file ON jobs(file_id);
