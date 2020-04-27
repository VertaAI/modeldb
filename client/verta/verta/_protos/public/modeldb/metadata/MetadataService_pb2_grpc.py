# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import grpc

from ...modeldb.metadata import MetadataService_pb2 as modeldb_dot_metadata_dot_MetadataService__pb2


class MetadataServiceStub(object):
  # missing associated documentation comment in .proto file
  pass

  def __init__(self, channel):
    """Constructor.

    Args:
      channel: A grpc.Channel.
    """
    self.GetLabels = channel.unary_unary(
        '/ai.verta.modeldb.metadata.MetadataService/GetLabels',
        request_serializer=modeldb_dot_metadata_dot_MetadataService__pb2.GetLabelsRequest.SerializeToString,
        response_deserializer=modeldb_dot_metadata_dot_MetadataService__pb2.GetLabelsRequest.Response.FromString,
        )
    self.AddLabels = channel.unary_unary(
        '/ai.verta.modeldb.metadata.MetadataService/AddLabels',
        request_serializer=modeldb_dot_metadata_dot_MetadataService__pb2.AddLabelsRequest.SerializeToString,
        response_deserializer=modeldb_dot_metadata_dot_MetadataService__pb2.AddLabelsRequest.Response.FromString,
        )
    self.DeleteLabels = channel.unary_unary(
        '/ai.verta.modeldb.metadata.MetadataService/DeleteLabels',
        request_serializer=modeldb_dot_metadata_dot_MetadataService__pb2.DeleteLabelsRequest.SerializeToString,
        response_deserializer=modeldb_dot_metadata_dot_MetadataService__pb2.DeleteLabelsRequest.Response.FromString,
        )


class MetadataServiceServicer(object):
  # missing associated documentation comment in .proto file
  pass

  def GetLabels(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def AddLabels(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def DeleteLabels(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')


def add_MetadataServiceServicer_to_server(servicer, server):
  rpc_method_handlers = {
      'GetLabels': grpc.unary_unary_rpc_method_handler(
          servicer.GetLabels,
          request_deserializer=modeldb_dot_metadata_dot_MetadataService__pb2.GetLabelsRequest.FromString,
          response_serializer=modeldb_dot_metadata_dot_MetadataService__pb2.GetLabelsRequest.Response.SerializeToString,
      ),
      'AddLabels': grpc.unary_unary_rpc_method_handler(
          servicer.AddLabels,
          request_deserializer=modeldb_dot_metadata_dot_MetadataService__pb2.AddLabelsRequest.FromString,
          response_serializer=modeldb_dot_metadata_dot_MetadataService__pb2.AddLabelsRequest.Response.SerializeToString,
      ),
      'DeleteLabels': grpc.unary_unary_rpc_method_handler(
          servicer.DeleteLabels,
          request_deserializer=modeldb_dot_metadata_dot_MetadataService__pb2.DeleteLabelsRequest.FromString,
          response_serializer=modeldb_dot_metadata_dot_MetadataService__pb2.DeleteLabelsRequest.Response.SerializeToString,
      ),
  }
  generic_handler = grpc.method_handlers_generic_handler(
      'ai.verta.modeldb.metadata.MetadataService', rpc_method_handlers)
  server.add_generic_rpc_handlers((generic_handler,))
