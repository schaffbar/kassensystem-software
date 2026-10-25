import { SchemaPath, validate } from '@angular/forms/signals';

import { Tool } from './tool.model';

/**
 * Validates that no other tool already uses the name (case-insensitive).
 * The backend rejects duplicate names as well (`ToolNameAlreadyUsedException`, unique constraint on `tool.name`).
 *
 * @param tools all tools – read inside the validator, so with a signal the check re-runs when the tool list changes
 * @param ownToolId when editing an existing tool: its id, so the tool does not conflict with its own name
 */
export function validateUniqueToolName(path: SchemaPath<string>, tools: () => Tool[], ownToolId?: () => string): void {
  validate(path, ({ value }) => {
    const name = value().toLowerCase();
    if (!name) return undefined;

    const excludedId = ownToolId?.();
    const exists = tools().some((tool) => tool.id !== excludedId && tool.name.toLowerCase() === name);
    return exists ? { kind: 'nameExists', message: 'tools.dialogs.addTool.errors.nameExists' } : undefined;
  });
}
