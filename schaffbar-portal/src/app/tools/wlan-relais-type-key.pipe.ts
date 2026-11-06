import { Pipe, PipeTransform } from '@angular/core';

import { WlanRelaisType } from '../shared/models/wlan-relais-type';

/**
 * Translation key of a WLAN relais type – `null` for a tool without relais, so `orDash` shows a dash.
 *
 * `{{ tool.wlanRelaisType | wlanRelaisTypeKey | translate | orDash }}`
 */
@Pipe({ name: 'wlanRelaisTypeKey' })
export class WlanRelaisTypeKeyPipe implements PipeTransform {
  transform(type: WlanRelaisType | null): string | null {
    return type ? `tools.wlanRelaisTypes.${type}` : null;
  }
}
