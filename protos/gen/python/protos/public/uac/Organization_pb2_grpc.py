# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import grpc

from ..uac import Organization_pb2 as uac_dot_Organization__pb2
from ..uac import UACService_pb2 as uac_dot_UACService__pb2


class OrganizationServiceStub(object):
  # missing associated documentation comment in .proto file
  pass

  def __init__(self, channel):
    """Constructor.

    Args:
      channel: A grpc.Channel.
    """
    self.getOrganizationById = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/getOrganizationById',
        request_serializer=uac_dot_Organization__pb2.GetOrganizationById.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.GetOrganizationById.Response.FromString,
        )
    self.getOrganizationByName = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/getOrganizationByName',
        request_serializer=uac_dot_Organization__pb2.GetOrganizationByName.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.GetOrganizationByName.Response.FromString,
        )
    self.getOrganizationByShortName = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/getOrganizationByShortName',
        request_serializer=uac_dot_Organization__pb2.GetOrganizationByShortName.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.GetOrganizationByShortName.Response.FromString,
        )
    self.listOrganizations = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/listOrganizations',
        request_serializer=uac_dot_Organization__pb2.ListOrganizations.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.ListOrganizations.Response.FromString,
        )
    self.listMyOrganizations = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/listMyOrganizations',
        request_serializer=uac_dot_Organization__pb2.ListMyOrganizations.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.ListMyOrganizations.Response.FromString,
        )
    self.setOrganization = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/setOrganization',
        request_serializer=uac_dot_Organization__pb2.SetOrganization.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.SetOrganization.Response.FromString,
        )
    self.deleteOrganization = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/deleteOrganization',
        request_serializer=uac_dot_Organization__pb2.DeleteOrganization.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.DeleteOrganization.Response.FromString,
        )
    self.listTeams = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/listTeams',
        request_serializer=uac_dot_Organization__pb2.ListTeams.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.ListTeams.Response.FromString,
        )
    self.listUsers = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/listUsers',
        request_serializer=uac_dot_Organization__pb2.ListUsers.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.ListUsers.Response.FromString,
        )
    self.addUser = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/addUser',
        request_serializer=uac_dot_Organization__pb2.AddUser.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.AddUser.Response.FromString,
        )
    self.removeUser = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/removeUser',
        request_serializer=uac_dot_Organization__pb2.RemoveUser.SerializeToString,
        response_deserializer=uac_dot_Organization__pb2.RemoveUser.Response.FromString,
        )
    self.addAdmins = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/addAdmins',
        request_serializer=uac_dot_Organization__pb2.ModifyOrganizationAdmins.SerializeToString,
        response_deserializer=uac_dot_UACService__pb2.Empty.FromString,
        )
    self.removeAdmins = channel.unary_unary(
        '/ai.verta.uac.OrganizationService/removeAdmins',
        request_serializer=uac_dot_Organization__pb2.ModifyOrganizationAdmins.SerializeToString,
        response_deserializer=uac_dot_UACService__pb2.Empty.FromString,
        )


class OrganizationServiceServicer(object):
  # missing associated documentation comment in .proto file
  pass

  def getOrganizationById(self, request, context):
    """Gets information from a given organization
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getOrganizationByName(self, request, context):
    """Gets information from a given organization
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getOrganizationByShortName(self, request, context):
    """Gets information from a given organization
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def listOrganizations(self, request, context):
    """Lists the organizations that the current user can access
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def listMyOrganizations(self, request, context):
    """Lists the organizations that the current user is a member of
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def setOrganization(self, request, context):
    """Create or update an organization
    Automatically sets the user making the call as owner and adds to the organization
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def deleteOrganization(self, request, context):
    """Delete an existing organization
    Only enabled if the person deleting is the creator
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def listTeams(self, request, context):
    """List teams that are part of an organization
    Only available for users inside the organization itself
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def listUsers(self, request, context):
    """List users inside an organization
    Only available for users inside the organization itself
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def addUser(self, request, context):
    """Adds the given user to the organization
    Only enabled if the requester is the creator of the organization
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def removeUser(self, request, context):
    """Removes the given user to the organization
    Only enabled if the requester is the creator of the organization
    The owner can never be removed
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def addAdmins(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def removeAdmins(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')


def add_OrganizationServiceServicer_to_server(servicer, server):
  rpc_method_handlers = {
      'getOrganizationById': grpc.unary_unary_rpc_method_handler(
          servicer.getOrganizationById,
          request_deserializer=uac_dot_Organization__pb2.GetOrganizationById.FromString,
          response_serializer=uac_dot_Organization__pb2.GetOrganizationById.Response.SerializeToString,
      ),
      'getOrganizationByName': grpc.unary_unary_rpc_method_handler(
          servicer.getOrganizationByName,
          request_deserializer=uac_dot_Organization__pb2.GetOrganizationByName.FromString,
          response_serializer=uac_dot_Organization__pb2.GetOrganizationByName.Response.SerializeToString,
      ),
      'getOrganizationByShortName': grpc.unary_unary_rpc_method_handler(
          servicer.getOrganizationByShortName,
          request_deserializer=uac_dot_Organization__pb2.GetOrganizationByShortName.FromString,
          response_serializer=uac_dot_Organization__pb2.GetOrganizationByShortName.Response.SerializeToString,
      ),
      'listOrganizations': grpc.unary_unary_rpc_method_handler(
          servicer.listOrganizations,
          request_deserializer=uac_dot_Organization__pb2.ListOrganizations.FromString,
          response_serializer=uac_dot_Organization__pb2.ListOrganizations.Response.SerializeToString,
      ),
      'listMyOrganizations': grpc.unary_unary_rpc_method_handler(
          servicer.listMyOrganizations,
          request_deserializer=uac_dot_Organization__pb2.ListMyOrganizations.FromString,
          response_serializer=uac_dot_Organization__pb2.ListMyOrganizations.Response.SerializeToString,
      ),
      'setOrganization': grpc.unary_unary_rpc_method_handler(
          servicer.setOrganization,
          request_deserializer=uac_dot_Organization__pb2.SetOrganization.FromString,
          response_serializer=uac_dot_Organization__pb2.SetOrganization.Response.SerializeToString,
      ),
      'deleteOrganization': grpc.unary_unary_rpc_method_handler(
          servicer.deleteOrganization,
          request_deserializer=uac_dot_Organization__pb2.DeleteOrganization.FromString,
          response_serializer=uac_dot_Organization__pb2.DeleteOrganization.Response.SerializeToString,
      ),
      'listTeams': grpc.unary_unary_rpc_method_handler(
          servicer.listTeams,
          request_deserializer=uac_dot_Organization__pb2.ListTeams.FromString,
          response_serializer=uac_dot_Organization__pb2.ListTeams.Response.SerializeToString,
      ),
      'listUsers': grpc.unary_unary_rpc_method_handler(
          servicer.listUsers,
          request_deserializer=uac_dot_Organization__pb2.ListUsers.FromString,
          response_serializer=uac_dot_Organization__pb2.ListUsers.Response.SerializeToString,
      ),
      'addUser': grpc.unary_unary_rpc_method_handler(
          servicer.addUser,
          request_deserializer=uac_dot_Organization__pb2.AddUser.FromString,
          response_serializer=uac_dot_Organization__pb2.AddUser.Response.SerializeToString,
      ),
      'removeUser': grpc.unary_unary_rpc_method_handler(
          servicer.removeUser,
          request_deserializer=uac_dot_Organization__pb2.RemoveUser.FromString,
          response_serializer=uac_dot_Organization__pb2.RemoveUser.Response.SerializeToString,
      ),
      'addAdmins': grpc.unary_unary_rpc_method_handler(
          servicer.addAdmins,
          request_deserializer=uac_dot_Organization__pb2.ModifyOrganizationAdmins.FromString,
          response_serializer=uac_dot_UACService__pb2.Empty.SerializeToString,
      ),
      'removeAdmins': grpc.unary_unary_rpc_method_handler(
          servicer.removeAdmins,
          request_deserializer=uac_dot_Organization__pb2.ModifyOrganizationAdmins.FromString,
          response_serializer=uac_dot_UACService__pb2.Empty.SerializeToString,
      ),
  }
  generic_handler = grpc.method_handlers_generic_handler(
      'ai.verta.uac.OrganizationService', rpc_method_handlers)
  server.add_generic_rpc_handlers((generic_handler,))
