import { CustomerApiModel } from '../shared/api/customers/customer.api-model';
import { User } from './user.model';

// ----------------------------------------------------------------------------
// API model → feature model

export function toUser(apiModel: CustomerApiModel): User {
  return {
    id: apiModel.id,
    firstName: apiModel.firstName,
    lastName: apiModel.lastName,
    email: apiModel.email,
  };
}
