import { maxLength, SchemaPath, validate } from '@angular/forms/signals';

// the database column `tool.description` holds 255 characters
export const TOOL_DESCRIPTION_MAX_LENGTH = 255;

export function validateToolDescription(path: SchemaPath<string>): void {
  maxLength(path, TOOL_DESCRIPTION_MAX_LENGTH);
}

/**
 * Validates that no other tool already uses the name (case-insensitive).
 * The backend rejects duplicate names as well (`ToolNameAlreadyUsedException`, unique constraint on `tool.name`).
 *
 * @param tools all tools – read inside the validator, so with a signal the check re-runs when the tool list changes
 * @param ownToolId when editing an existing tool: its id, so the tool does not conflict with its own name
 */
export function validateUniqueToolName(
  path: SchemaPath<string>,
  tools: () => { id: string; name: string }[],
  ownToolId?: () => string,
): void {
  validate(path, ({ value }) => {
    const name = value().toLowerCase();
    if (!name) return undefined;

    const excludedId = ownToolId?.();
    const exists = tools().some((tool) => tool.id !== excludedId && tool.name.toLowerCase() === name);
    return exists ? { kind: 'nameExists', message: 'tools.dialogs.addTool.errors.nameExists' } : undefined;
  });
}
