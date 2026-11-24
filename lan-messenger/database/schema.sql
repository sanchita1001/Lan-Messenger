-- LAN Messenger Database Schema
-- MySQL 8.0+
-- Character Set: UTF-8 (utf8mb4) for full Unicode support including emojis

-- Drop existing database if exists (use with caution!)
-- DROP DATABASE IF EXISTS lan_messenger;

-- Create database
CREATE DATABASE IF NOT EXISTS lan_messenger 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE lan_messenger;

-- ============================================================================
-- TABLE: users
-- Stores user account information with hashed passwords
-- ============================================================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_last_seen (last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: conversations
-- Stores both private (1-on-1) and group conversations
-- ============================================================================
CREATE TABLE conversations (
    conversation_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_type ENUM('PRIVATE', 'GROUP') NOT NULL,
    conversation_name VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_type (conversation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: conversation_members
-- Many-to-many relationship between users and conversations
-- ============================================================================
CREATE TABLE conversation_members (
    conversation_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (conversation_id, user_id),
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_conversations (user_id, conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================================
CREATE TABLE messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_conversation_time (conversation_id, sent_at DESC),
    FULLTEXT INDEX idx_message_search (message_text)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: files
-- Stores metadata for file transfers (actual files stored on disk)
-- ============================================================================
CREATE TABLE files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT UNIQUE NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
    
    INDEX idx_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- SAMPLE DATA FOR TESTING
-- ============================================================================

-- Insert test users (passwords are "password123" hashed with BCrypt)
-- You should use the application's registration to create real users
INSERT INTO users (username, password_hash) VALUES
('alice', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYhKJgVqiKS'),
('bob', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYhKJgVqiKS'),
('charlie', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYhKJgVqiKS');

-- Create a private conversation between alice and bob
INSERT INTO conversations (conversation_type, conversation_name) VALUES
('PRIVATE', NULL);

-- Add alice and bob as members
INSERT INTO conversation_members (conversation_id, user_id) VALUES
(1, 1),  -- alice in conversation 1
(1, 2);  -- bob in conversation 1

-- Create a group conversation
INSERT INTO conversations (conversation_type, conversation_name) VALUES
('GROUP', 'Team Chat');

-- Add all three users to the group
INSERT INTO conversation_members (conversation_id, user_id) VALUES
(2, 1),  -- alice in conversation 2
(2, 2),  -- bob in conversation 2
(2, 3);  -- charlie in conversation 2

-- Add sample messages
INSERT INTO messages (conversation_id, sender_id, message_text) VALUES
(1, 1, 'Hey Bob, how are you?'),
(1, 2, 'Hi Alice! I\'m doing great, thanks!'),
(1, 1, 'That\'s wonderful to hear!'),
(2, 1, 'Welcome to the team chat everyone!'),
(2, 2, 'Thanks Alice! Glad to be here.'),
(2, 3, 'Hello team! 👋');


SHOW TABLES;

-- Show table structures
DESCRIBE users;
DESCRIBE conversations;
DESCRIBE conversation_members;
DESCRIBE messages;
DESCRIBE files;

-- Count records
SELECT 'Users' as table_name, COUNT(*) as record_count FROM users
UNION ALL
SELECT 'Conversations', COUNT(*) FROM conversations
UNION ALL
SELECT 'Conversation Members', COUNT(*) FROM conversation_members
UNION ALL
SELECT 'Messages', COUNT(*) FROM messages
UNION ALL
SELECT 'Files', COUNT(*) FROM files;

