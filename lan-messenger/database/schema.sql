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
-- TABLE: messages
-- Stores all chat messages
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

-- ============================================================================
-- USEFUL QUERIES FOR APPLICATION DEVELOPMENT
-- ============================================================================

-- Query 1: Check if username exists
-- SELECT COUNT(*) FROM users WHERE username = 'alice';

-- Query 2: Get user by username for login
-- SELECT user_id, username, password_hash FROM users WHERE username = 'alice';

-- Query 3: Get all conversations for a user
-- SELECT c.conversation_id, c.conversation_type, c.conversation_name, c.created_at
-- FROM conversations c
-- INNER JOIN conversation_members cm ON c.conversation_id = cm.conversation_id
-- WHERE cm.user_id = 1
-- ORDER BY c.created_at DESC;

-- Query 4: Get conversation members
-- SELECT u.user_id, u.username, u.last_seen
-- FROM users u
-- INNER JOIN conversation_members cm ON u.user_id = cm.user_id
-- WHERE cm.conversation_id = 1;

-- Query 5: Get recent messages for a conversation
-- SELECT m.message_id, m.sender_id, u.username as sender_name, m.message_text, m.sent_at
-- FROM messages m
-- INNER JOIN users u ON m.sender_id = u.user_id
-- WHERE m.conversation_id = 1
-- ORDER BY m.sent_at DESC
-- LIMIT 50;

-- Query 6: Check if user is member of conversation
-- SELECT COUNT(*) FROM conversation_members 
-- WHERE conversation_id = 1 AND user_id = 1;

-- Query 7: Search messages in a conversation
-- SELECT m.message_id, u.username, m.message_text, m.sent_at
-- FROM messages m
-- INNER JOIN users u ON m.sender_id = u.user_id
-- WHERE m.conversation_id = 1 
-- AND MATCH(m.message_text) AGAINST('search term' IN NATURAL LANGUAGE MODE)
-- ORDER BY m.sent_at DESC;

-- Query 8: Get online users (active in last 5 minutes)
-- SELECT user_id, username, last_seen
-- FROM users
-- WHERE last_seen >= NOW() - INTERVAL 5 MINUTE
-- ORDER BY last_seen DESC;

-- Query 9: Find or create private conversation between two users
-- SELECT c.conversation_id
-- FROM conversations c
-- WHERE c.conversation_type = 'PRIVATE'
-- AND c.conversation_id IN (
--     SELECT conversation_id FROM conversation_members WHERE user_id = 1
-- )
-- AND c.conversation_id IN (
--     SELECT conversation_id FROM conversation_members WHERE user_id = 2
-- )
-- AND (
--     SELECT COUNT(*) FROM conversation_members WHERE conversation_id = c.conversation_id
-- ) = 2;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Show all tables
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

-- ============================================================================
-- CLEANUP (Use with caution - deletes all data!)
-- ============================================================================

-- To reset the database:
-- DROP TABLE IF EXISTS files;
-- DROP TABLE IF EXISTS messages;
-- DROP TABLE IF EXISTS conversation_members;
-- DROP TABLE IF EXISTS conversations;
-- DROP TABLE IF EXISTS users;
