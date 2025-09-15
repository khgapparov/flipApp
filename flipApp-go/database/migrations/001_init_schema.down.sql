-- Drop tables in reverse order of creation to handle foreign key constraints
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS properties;
DROP TABLE IF EXISTS users;

-- Drop indexes
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_user_id;
DROP INDEX IF EXISTS idx_refresh_tokens_token;
DROP INDEX IF EXISTS idx_refresh_tokens_user_id;
DROP INDEX IF EXISTS idx_properties_property_id;
DROP INDEX IF EXISTS idx_properties_city_state;
