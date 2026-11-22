-- The services check these rules before they save, but two requests at the same time (a reader that sends a tag
-- twice) can both pass the check. The partial unique indexes let the second one fail in the database – it answers
-- 500 (a device shows its general error); it should hardly ever happen, so it is not mapped to the rule's code.
-- Checked before: the real database had no duplicates and no session without status (22.11.2026).

-- WU-1: at most one running workshop usage per customer
CREATE UNIQUE INDEX uq_workshop_usage_running ON schaffbar.workshop_usage (customer_id) WHERE exit_time IS NULL;

-- WS-1: at most one open workshop session per customer; the index relies on the status, which the entity always sets
ALTER TABLE schaffbar.workshop_session ALTER COLUMN status SET NOT NULL;
CREATE UNIQUE INDEX uq_workshop_session_open ON schaffbar.workshop_session (customer_id) WHERE status = 'OPEN';

-- TU-9: at most one running usage per tool – this also covers TU-3 (the same customer twice at the same tool)
CREATE UNIQUE INDEX uq_tool_usage_running ON schaffbar.tool_usage (tool_id) WHERE end_time IS NULL;
