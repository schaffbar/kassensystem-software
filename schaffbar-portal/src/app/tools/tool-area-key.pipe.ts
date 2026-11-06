import { Pipe, PipeTransform } from '@angular/core';

import { ToolArea } from '../shared/models/tool-area';

/**
 * Translation key of a tool area.
 *
 * `{{ tool.area | toolAreaKey | translate }}`
 */
@Pipe({ name: 'toolAreaKey' })
export class ToolAreaKeyPipe implements PipeTransform {
  transform(area: ToolArea): string {
    return `tools.areas.${area}`;
  }
}
