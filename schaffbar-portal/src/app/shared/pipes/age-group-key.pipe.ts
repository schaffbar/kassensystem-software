import { Pipe, PipeTransform } from '@angular/core';

import { AgeGroup } from '../models/age-group';

/**
 * Translation key of an age group.
 *
 * `{{ user.ageGroup | ageGroupKey | translate }}`
 */
@Pipe({ name: 'ageGroupKey' })
export class AgeGroupKeyPipe implements PipeTransform {
  transform(ageGroup: AgeGroup): string {
    return `shared.ageGroups.${ageGroup.toLowerCase()}`;
  }
}
