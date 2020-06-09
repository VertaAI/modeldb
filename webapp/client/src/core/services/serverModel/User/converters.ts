import User, { CurrentUser, unknownUser } from 'core/shared/models/User';
import { IServerUserInfo } from 'core/services/serverModel/User/User';

export const convertServerCurrentUser = (serverUser: IServerUserInfo) => {
  return new CurrentUser({
    id: serverUser.verta_info.user_id,
    email: serverUser.email,
    username: serverUser.verta_info.username,
    dateLastLoggedIn: new Date(Number(serverUser.verta_info.refresh_timestamp)),
    developerKey: serverUser.dev_key,
    fullName: serverUser.full_name || serverUser.first_name,
    picture: serverUser.image_url,
  });
};

export const convertServerUser = (serverUser: IServerUserInfo) => {
  if (!serverUser) {
    return unknownUser;
  }
  const user = new User({
    id: serverUser.verta_info.user_id,
    email: serverUser.email,
    username: serverUser.verta_info.username,
    fullName: serverUser.full_name || serverUser.first_name,
    picture: serverUser.image_url,
  });
  return user;
};
