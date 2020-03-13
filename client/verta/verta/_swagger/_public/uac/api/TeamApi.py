# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class TeamApi:
  def __init__(self, client, base_path = "/v1"):
    self.client = client
    self.base_path = base_path

  def addUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/team/addUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacAddTeamUserResponse import UacAddTeamUserResponse
      ret = UacAddTeamUserResponse.from_json(ret)

    return ret

  def deleteTeam(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/team/deleteTeam"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacDeleteTeamResponse import UacDeleteTeamResponse
      ret = UacDeleteTeamResponse.from_json(ret)

    return ret

  def getTeamById(self, team_id=None):
    __query = {
      "team_id": client.to_query(team_id)
    }
    body = None

    format_args = {}
    path = "/team/getTeamById"
    if "$team_id" in path:
      path = path.replace("$team_id", "%(team_id)s")
      format_args["team_id"] = team_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetTeamByIdResponse import UacGetTeamByIdResponse
      ret = UacGetTeamByIdResponse.from_json(ret)

    return ret

  def getTeamByName(self, org_id=None, team_name=None):
    __query = {
      "org_id": client.to_query(org_id),
      "team_name": client.to_query(team_name)
    }
    body = None

    format_args = {}
    path = "/team/getTeamByName"
    if "$org_id" in path:
      path = path.replace("$org_id", "%(org_id)s")
      format_args["org_id"] = org_id
    if "$team_name" in path:
      path = path.replace("$team_name", "%(team_name)s")
      format_args["team_name"] = team_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetTeamByNameResponse import UacGetTeamByNameResponse
      ret = UacGetTeamByNameResponse.from_json(ret)

    return ret

  def getTeamByShortName(self, org_id=None, short_name=None):
    __query = {
      "org_id": client.to_query(org_id),
      "short_name": client.to_query(short_name)
    }
    body = None

    format_args = {}
    path = "/team/getTeamByShortName"
    if "$org_id" in path:
      path = path.replace("$org_id", "%(org_id)s")
      format_args["org_id"] = org_id
    if "$short_name" in path:
      path = path.replace("$short_name", "%(short_name)s")
      format_args["short_name"] = short_name
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacGetTeamByShortNameResponse import UacGetTeamByShortNameResponse
      ret = UacGetTeamByShortNameResponse.from_json(ret)

    return ret

  def listMyTeams(self, ):
    __query = {
    }
    body = None

    format_args = {}
    path = "/team/listMyTeams"
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListMyTeamsResponse import UacListMyTeamsResponse
      ret = UacListMyTeamsResponse.from_json(ret)

    return ret

  def listUsers(self, team_id=None):
    __query = {
      "team_id": client.to_query(team_id)
    }
    body = None

    format_args = {}
    path = "/team/listUsers"
    if "$team_id" in path:
      path = path.replace("$team_id", "%(team_id)s")
      format_args["team_id"] = team_id
    ret = self.client.request("GET", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacListTeamUserResponse import UacListTeamUserResponse
      ret = UacListTeamUserResponse.from_json(ret)

    return ret

  def removeUser(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/team/removeUser"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacRemoveTeamUserResponse import UacRemoveTeamUserResponse
      ret = UacRemoveTeamUserResponse.from_json(ret)

    return ret

  def setTeam(self, body=None):
    __query = {
    }
    if body is None:
      raise Exception("Missing required parameter \"body\"")

    format_args = {}
    path = "/team/setTeam"
    if "$body" in path:
      path = path.replace("$body", "%(body)s")
      format_args["body"] = body
    ret = self.client.request("POST", self.base_path + path % format_args, __query, body)
    if ret is not None:
      from ..model.UacSetTeamResponse import UacSetTeamResponse
      ret = UacSetTeamResponse.from_json(ret)

    return ret
