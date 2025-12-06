/******************************************************************************
 * Sentence Builder Database Schema
 *
 * This script creates the full database schema used by the Sentence Builder
 * application. It defines all core tables:
 *
 *   - files:            tracks imported text files
 *   - words:            stores vocabulary terms + global usage statistics
 *   - word_relations_bigram:  bigram (two-word) transitions + frequencies
 *   - word_relations_trigram: trigram (three-word) transitions + frequencies
 *   - word_files:       per-file word frequencies and file/word relationships
 *
 * Notes:
 *   • The unused metadata table has been removed.
 *   • The unused part_of_speech column has been removed from words.
 *   • All relationship tables use ON DELETE CASCADE so clearing the database
 *     is fast and consistent.
 *
 * Written by Mehdi for CS 4485.0W1,
 * Started Sept 2025.
 ******************************************************************************/

-- Drop and recreate the database
DROP DATABASE IF EXISTS sentencegen;
CREATE DATABASE sentencegen;
USE sentencegen;


-- Table: files
-- Tracks every imported text file, the number of words processed, and when
-- the import occurred. ImportService inserts into this table once per file.
CREATE TABLE files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL UNIQUE,
    word_count INT NOT NULL,
    import_date DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- Table: words
-- Stores each unique normalized word discovered during import.
--
-- Columns:
--   word_text:  the lowercase normalized word itself
--   total_count: total occurrences across ALL files
--   sentence_start_count: occurrences of the word starting a sentence
--   sentence_end_count:   occurrences of the word ending a sentence
--
-- Removed:
--   part_of_speech — unused by the application
CREATE TABLE words (
    word_id INT AUTO_INCREMENT PRIMARY KEY,
    word_text VARCHAR(100) NOT NULL UNIQUE,
    total_count INT DEFAULT 0,
    sentence_start_count INT DEFAULT 0,
    sentence_end_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- Table: word_relations_bigram
-- Stores directional (A → B) word transitions and how often they occur.
--
-- Used by:
--  Bigram-based autocomplete
--  Bigram-based sentence generation algorithms
CREATE TABLE word_relations_bigram (
    from_word_id INT NOT NULL,
    to_word_id INT NOT NULL,
    frequency INT DEFAULT 1,
    PRIMARY KEY (from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id)   REFERENCES words(word_id) ON DELETE CASCADE
);


-- Table: word_relations_trigram
-- Stores three-word transitions (A B → C), enabling context-aware prediction.
--
-- Used by:
--  Trigram autocomplete algorithms
--  Trigram-based sentence generators
CREATE TABLE word_relations_trigram (
    first_word_id INT NOT NULL,
    second_word_id INT NOT NULL,
    next_word_id INT NOT NULL,
    frequency INT DEFAULT 1,
    PRIMARY KEY (first_word_id, second_word_id, next_word_id),
    FOREIGN KEY (first_word_id)  REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (second_word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (next_word_id)   REFERENCES words(word_id) ON DELETE CASCADE
);


-- Table: word_files
-- Connects words to the specific files they appear in.
-- This allows analytics such as “word usage per file” and helps maintain
-- accurate global counts during imports.
CREATE TABLE word_files (
    word_id INT NOT NULL,
    file_id INT NOT NULL,
    count_in_file INT DEFAULT 0,
    PRIMARY KEY (word_id, file_id),
    FOREIGN KEY (word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE
);

