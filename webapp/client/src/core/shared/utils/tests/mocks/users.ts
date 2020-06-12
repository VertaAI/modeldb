import User, { CurrentUser } from "models/User";

export const currentUser: CurrentUser = new CurrentUser({
  id: 'current-user-id',
  dateLastLoggedIn: new Date(),
  developerKey: 'developerKey',
  email: 'rokill@sibmail.com',
  username: 'current-user-username',
});

export const user: User = new User({
  id: 'user-id',
  email: 'gusfgsfg@sibmail.com',
  username: 'user-username',
});

export const users = [
  user,
  new User({
    id: 'user-id-2',
    email: 'user-email-2@sibmail.com',
    username: 'user-username-2',
  }),
  new User({
    id: 'user-id-3',
    email: 'user-email-3@sibmail.com',
    username: 'user-username-3',
  }),
];
