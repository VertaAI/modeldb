const dotenv = require('dotenv');
dotenv.config();

const getConfig = () => {
  return {
    baseURL: process.env.BASE_URL,
    backend: {
      domain: process.env.BACKEND_API_DOMAIN,
    },
    github2FASecret: process.env.GITHUB_SECRET,
    user: {
      email: process.env.USER_EMAIL,
      developerKey: process.env.USER_DEVELOPER_KEY,
      password: process.env.USER_PASSWORD,
      username: process.env.USER_USERNAME,
      workspace: 'personal',
    },
    anotherUser: {
      email: process.env.ANOTHER_USER_EMAIL,
      developerKey: process.env.ANOTHER_USER_DEVELOPER_KEY,
      username: process.env.ANOTHER_USER_USERNAME,
    },
  };
};

module.exports = getConfig;
