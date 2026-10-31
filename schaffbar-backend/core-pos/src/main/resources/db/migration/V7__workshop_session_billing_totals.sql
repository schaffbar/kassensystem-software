-- Final billing totals of a session, stored when the session is closed.
-- Each usage is rounded on its own (minutes rounded up, one unit per started 6 minutes), the totals are the sums.
-- Null while the session is open – and for sessions closed before this migration, their totals are unknown.

ALTER TABLE schaffbar.workshop_session ADD COLUMN workshop_minutes BIGINT;
ALTER TABLE schaffbar.workshop_session ADD COLUMN workshop_units BIGINT;
ALTER TABLE schaffbar.workshop_session ADD COLUMN tool_minutes BIGINT;
ALTER TABLE schaffbar.workshop_session ADD COLUMN tool_units BIGINT;
