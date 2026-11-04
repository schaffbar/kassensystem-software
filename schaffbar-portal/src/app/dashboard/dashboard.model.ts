export interface ActiveUser {
  customerId: string;
  firstName: string;
  lastName: string;
  entryTime: Date;
}

export interface ActiveTool {
  toolId: string;
  toolName: string;
  customerId: string;
  firstName: string;
  lastName: string;
}
