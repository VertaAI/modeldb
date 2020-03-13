# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class UACServiceApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def createUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/uac/createUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacCreateUserResponse import UacCreateUserResponse
      ret = UacCreateUserResponse.from_json(ret)

    return ret

  def deleteUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/uac/deleteUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacDeleteUserResponse import UacDeleteUserResponse
      ret = UacDeleteUserResponse.from_json(ret)

    return ret

  def getCurrentUser(self, ):
    __query = {
    }
    body = None

    format_args = {}
    path = "/uac/getCurrentUser"
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacUserInfo import UacUserInfo
      ret = UacUserInfo.from_json(ret)

    return ret

  def getUser(self, user_id=None, email=None, username=None):
    __query = {
      "user_id": client.to_query(user_id),
      "email": client.to_query(email),
      "username": client.to_query(username)
    }
    body = None

    format_args = {}
    path = "/uac/getUser"
    if "$user_id" in path:
      path = path.replace("$user_id", "%(user_id)s")
      format_args["user_id"] = user_id
    if "$email" in path:
      path = path.replace("$email", "%(email)s")
      format_args["email"] = email
    if "$username" in path:
      path = path.replace("$username", "%(username)s")
      format_args["username"] = username
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacUserInfo import UacUserInfo
      ret = UacUserInfo.from_json(ret)

    return ret

  def getUsers(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/uac/getUsers"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetUsersResponse import UacGetUsersResponse
      ret = UacGetUsersResponse.from_json(ret)

    return ret

  def updateUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/uac/updateUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacUpdateUserResponse import UacUpdateUserResponse
      ret = UacUpdateUserResponse.from_json(ret)

    return ret
