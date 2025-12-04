
/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE DATABASE file_management;
USE file_management;

/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE TABLE files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL UNIQUE,
    word_count INT NOT NULL,
    import_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE TABLE words (
    word_id INT AUTO_INCREMENT PRIMARY KEY,
    word_text VARCHAR(100) NOT NULL UNIQUE,
    total_count INT DEFAULT 0,
    sentence_start_count INT DEFAULT 0,
    sentence_end_count INT DEFAULT 0,
    part_of_speech VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE TABLE word_relations_bigram (
    from_word_id INT NOT NULL,
    to_word_id INT NOT NULL,
    frequency INT DEFAULT 1,
    PRIMARY KEY (from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES words(word_id) ON DELETE CASCADE
);

/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE TABLE word_relations_trigram (
    first_word_id INT NOT NULL,
    second_word_id INT NOT NULL,
    next_word_id INT NOT NULL,
    frequency INT DEFAULT 1,
    PRIMARY KEY (first_word_id, second_word_id, next_word_id),
    FOREIGN KEY (first_word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (second_word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (next_word_id) REFERENCES words(word_id) ON DELETE CASCADE
);

/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE TABLE word_files (
    word_id INT NOT NULL,
    file_id INT NOT NULL,
    count_in_file INT DEFAULT 0,
    PRIMARY KEY (word_id, file_id),
    FOREIGN KEY (word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE
);

/*
Written by <Mehdi Devjani> (<mmd210006>) and <Yousuf Ismail> (<YXI220002>) for <CS4485.0W1>, 
*/
CREATE TABLE metadata (
    meta_key VARCHAR(100) PRIMARY KEY,
    meta_value VARCHAR(255)
);


