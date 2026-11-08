import { Pipe, PipeTransform } from '@angular/core';

import { RfidTagAssignmentType } from '../shared/models/rfid-tag-assignment-type';

/**
 * Translation key of an assignment type – "Fest" or "Leihtag".
 *
 * `{{ assignee.assignmentType | rfidTagAssignmentTypeKey | translate }}`
 */
@Pipe({ name: 'rfidTagAssignmentTypeKey' })
export class RfidTagAssignmentTypeKeyPipe implements PipeTransform {
  transform(type: RfidTagAssignmentType): string {
    return `rfidTags.assignmentTypes.${type.toLowerCase()}`;
  }
}
