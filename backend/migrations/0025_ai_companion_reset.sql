-- Destructive one-time reset for the rebuilt AI companion product.
-- This intentionally removes legacy profiles, companions, chat history, and memories.
DELETE FROM ai_memory_items;
DELETE FROM ai_memory_settings;
DELETE FROM ai_chat_messages;
DELETE FROM ai_companions;
DELETE FROM ai_profiles;
