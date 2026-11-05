import { Pipe, PipeTransform } from '@angular/core';

import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';

/**
 * Translation key of an RFID tag assignment type.
 *
 * `{{ assignment.assignmentType | rfidTagAssignmentTypeKey | translate }}`
 */
@Pipe({ name: 'rfidTagAssignmentTypeKey' })
export class RfidTagAssignmentTypeKeyPipe implements PipeTransform {
  transform(type: RfidTagAssignmentType): string {
    return `users.chips.userDetails.rfidTags.${type.toLowerCase()}`;
  }
}
