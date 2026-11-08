import { Pipe, PipeTransform } from '@angular/core';

/**
 * A tag ID in groups of four hex characters, easier to compare with the print on the tag: `04A31F9C` → `04A3 1F9C`.
 *
 * `<span class="mono">{{ rfidTag.id | rfidTagId }}</span>`
 */
@Pipe({ name: 'rfidTagId' })
export class RfidTagIdPipe implements PipeTransform {
  transform(id: string): string {
    return id.match(/.{1,4}/g)?.join(' ') ?? id;
  }
}
