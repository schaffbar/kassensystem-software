-- The commands of a WLAN relais follow from its type (WlanRelaisType) and are no longer stored per tool.
-- Stored copies could differ from the type – old data, or a type whose commands changed later.

ALTER TABLE schaffbar.tool DROP COLUMN http_start_command;
ALTER TABLE schaffbar.tool DROP COLUMN on_command;
ALTER TABLE schaffbar.tool DROP COLUMN off_command;
