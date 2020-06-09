import { bind } from 'decko';
import * as R from 'ramda';

import { BaseDataService } from 'services/BaseDataService';
import User, { unknownUser } from 'core/shared/models/User';
import { convertServerUser } from 'core/services/serverModel/User/converters';

export default class UsersService extends BaseDataService {
  constructor() {
    super();
  }

  @bind
  public async loadUser(userId: string): Promise<User> {
    if (!userId) {
      return unknownUser;
    }
    const response = await this.get<any>({
      url: '/v1/uac-proxy/uac/getUser',
      config: {
        params: {
          user_id: userId,
        },
      },
    });

    return convertServerUser(response.data);
  }

  @bind
  public async loadUsers(userIds: string[]): Promise<User[]> {
    if (!userIds) {
      return [unknownUser];
    }
    if (userIds.every(R.isNil)) {
      return userIds.map(() => unknownUser);
    }
    if (userIds.length === 0) {
      return [];
    }
    const response = await this.post<any>({
      url: '/v1/uac-proxy/uac/getUsers',
      data: {
        user_ids: userIds,
      },
    });

    return (response.data.user_infos || []).map(convertServerUser);
  }
}
