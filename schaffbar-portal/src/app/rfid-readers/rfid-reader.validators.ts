import { SchemaPath, validate } from '@angular/forms/signals';

import { isRfidReaderNameUsed, RfidReader } from './rfid-reader.model';

// rfidReaders is read inside the validator – with a signal the check follows the list while the form is open
export function validateUniqueRfidReaderName(
  path: SchemaPath<string>,
  rfidReaderId: string,
  rfidReaders: () => Pick<RfidReader, 'id' | 'name'>[],
): void {
  validate(path, ({ value }) =>
    isRfidReaderNameUsed(value(), rfidReaderId, rfidReaders())
      ? { kind: 'nameExists', message: 'rfidReaders.dialogs.updateRfidReader.errors.nameExists' }
      : undefined,
  );
}
