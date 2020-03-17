export interface IServerUserInfo {
  user_id: string;
  email: string;
  dev_key: string;
  full_name?: string;
  first_name?: string;
  last_name?: string;
  image_url?: string;
  verta_info: {
    username: string;
    user_id: string;
    refresh_timestamp: string;
  };
}
