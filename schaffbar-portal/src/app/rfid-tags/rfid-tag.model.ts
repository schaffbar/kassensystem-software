export interface RfidTag {
  id: string;
  active: boolean;
}

// ----------------------------------------------------------------------------
// commands

export interface DeleteRfidTagCommand {
  id: string;
}
