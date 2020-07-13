# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
import grpc

from ..modeldb import DatasetVersionService_pb2 as modeldb_dot_DatasetVersionService__pb2


class DatasetVersionServiceStub(object):
  # missing associated documentation comment in .proto file
  pass

  def __init__(self, channel):
    """Constructor.

    Args:
      channel: A grpc.Channel.
    """
    self.createDatasetVersion = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/createDatasetVersion',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.CreateDatasetVersion.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.CreateDatasetVersion.Response.FromString,
        )
    self.getAllDatasetVersionsByDatasetId = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/getAllDatasetVersionsByDatasetId',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.GetAllDatasetVersionsByDatasetId.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.GetAllDatasetVersionsByDatasetId.Response.FromString,
        )
    self.deleteDatasetVersion = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/deleteDatasetVersion',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersion.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersion.Response.FromString,
        )
    self.deleteDatasetVersions = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/deleteDatasetVersions',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersions.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersions.Response.FromString,
        )
    self.getLatestDatasetVersionByDatasetId = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/getLatestDatasetVersionByDatasetId',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.GetLatestDatasetVersionByDatasetId.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.GetLatestDatasetVersionByDatasetId.Response.FromString,
        )
    self.findDatasetVersions = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/findDatasetVersions',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.FindDatasetVersions.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.FindDatasetVersions.Response.FromString,
        )
    self.updateDatasetVersionDescription = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/updateDatasetVersionDescription',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionDescription.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionDescription.Response.FromString,
        )
    self.addDatasetVersionTags = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/addDatasetVersionTags',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionTags.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionTags.Response.FromString,
        )
    self.deleteDatasetVersionTags = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/deleteDatasetVersionTags',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionTags.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionTags.Response.FromString,
        )
    self.addDatasetVersionAttributes = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/addDatasetVersionAttributes',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionAttributes.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionAttributes.Response.FromString,
        )
    self.updateDatasetVersionAttributes = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/updateDatasetVersionAttributes',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionAttributes.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionAttributes.Response.FromString,
        )
    self.getDatasetVersionAttributes = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/getDatasetVersionAttributes',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.GetDatasetVersionAttributes.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.GetDatasetVersionAttributes.Response.FromString,
        )
    self.deleteDatasetVersionAttributes = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/deleteDatasetVersionAttributes',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionAttributes.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionAttributes.Response.FromString,
        )
    self.setDatasetVersionVisibility = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/setDatasetVersionVisibility',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.SetDatasetVersionVisibilty.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.SetDatasetVersionVisibilty.Response.FromString,
        )
    self.getUrlForDatasetBlobVersioned = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/getUrlForDatasetBlobVersioned',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.GetUrlForDatasetBlobVersioned.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.GetUrlForDatasetBlobVersioned.Response.FromString,
        )
    self.commitVersionedDatasetBlobArtifactPart = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/commitVersionedDatasetBlobArtifactPart',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.CommitVersionedDatasetBlobArtifactPart.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.CommitVersionedDatasetBlobArtifactPart.Response.FromString,
        )
    self.getCommittedVersionedDatasetBlobArtifactParts = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/getCommittedVersionedDatasetBlobArtifactParts',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.GetCommittedVersionedDatasetBlobArtifactParts.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.GetCommittedVersionedDatasetBlobArtifactParts.Response.FromString,
        )
    self.commitMultipartVersionedDatasetBlobArtifact = channel.unary_unary(
        '/ai.verta.modeldb.DatasetVersionService/commitMultipartVersionedDatasetBlobArtifact',
        request_serializer=modeldb_dot_DatasetVersionService__pb2.CommitMultipartVersionedDatasetBlobArtifact.SerializeToString,
        response_deserializer=modeldb_dot_DatasetVersionService__pb2.CommitMultipartVersionedDatasetBlobArtifact.Response.FromString,
        )


class DatasetVersionServiceServicer(object):
  # missing associated documentation comment in .proto file
  pass

  def createDatasetVersion(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getAllDatasetVersionsByDatasetId(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def deleteDatasetVersion(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def deleteDatasetVersions(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getLatestDatasetVersionByDatasetId(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def findDatasetVersions(self, request, context):
    """queries
    """
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def updateDatasetVersionDescription(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def addDatasetVersionTags(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def deleteDatasetVersionTags(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def addDatasetVersionAttributes(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def updateDatasetVersionAttributes(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getDatasetVersionAttributes(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def deleteDatasetVersionAttributes(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def setDatasetVersionVisibility(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getUrlForDatasetBlobVersioned(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def commitVersionedDatasetBlobArtifactPart(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def getCommittedVersionedDatasetBlobArtifactParts(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')

  def commitMultipartVersionedDatasetBlobArtifact(self, request, context):
    # missing associated documentation comment in .proto file
    pass
    context.set_code(grpc.StatusCode.UNIMPLEMENTED)
    context.set_details('Method not implemented!')
    raise NotImplementedError('Method not implemented!')


def add_DatasetVersionServiceServicer_to_server(servicer, server):
  rpc_method_handlers = {
      'createDatasetVersion': grpc.unary_unary_rpc_method_handler(
          servicer.createDatasetVersion,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.CreateDatasetVersion.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.CreateDatasetVersion.Response.SerializeToString,
      ),
      'getAllDatasetVersionsByDatasetId': grpc.unary_unary_rpc_method_handler(
          servicer.getAllDatasetVersionsByDatasetId,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.GetAllDatasetVersionsByDatasetId.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.GetAllDatasetVersionsByDatasetId.Response.SerializeToString,
      ),
      'deleteDatasetVersion': grpc.unary_unary_rpc_method_handler(
          servicer.deleteDatasetVersion,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersion.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersion.Response.SerializeToString,
      ),
      'deleteDatasetVersions': grpc.unary_unary_rpc_method_handler(
          servicer.deleteDatasetVersions,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersions.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersions.Response.SerializeToString,
      ),
      'getLatestDatasetVersionByDatasetId': grpc.unary_unary_rpc_method_handler(
          servicer.getLatestDatasetVersionByDatasetId,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.GetLatestDatasetVersionByDatasetId.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.GetLatestDatasetVersionByDatasetId.Response.SerializeToString,
      ),
      'findDatasetVersions': grpc.unary_unary_rpc_method_handler(
          servicer.findDatasetVersions,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.FindDatasetVersions.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.FindDatasetVersions.Response.SerializeToString,
      ),
      'updateDatasetVersionDescription': grpc.unary_unary_rpc_method_handler(
          servicer.updateDatasetVersionDescription,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionDescription.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionDescription.Response.SerializeToString,
      ),
      'addDatasetVersionTags': grpc.unary_unary_rpc_method_handler(
          servicer.addDatasetVersionTags,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionTags.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionTags.Response.SerializeToString,
      ),
      'deleteDatasetVersionTags': grpc.unary_unary_rpc_method_handler(
          servicer.deleteDatasetVersionTags,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionTags.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionTags.Response.SerializeToString,
      ),
      'addDatasetVersionAttributes': grpc.unary_unary_rpc_method_handler(
          servicer.addDatasetVersionAttributes,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionAttributes.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.AddDatasetVersionAttributes.Response.SerializeToString,
      ),
      'updateDatasetVersionAttributes': grpc.unary_unary_rpc_method_handler(
          servicer.updateDatasetVersionAttributes,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionAttributes.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.UpdateDatasetVersionAttributes.Response.SerializeToString,
      ),
      'getDatasetVersionAttributes': grpc.unary_unary_rpc_method_handler(
          servicer.getDatasetVersionAttributes,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.GetDatasetVersionAttributes.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.GetDatasetVersionAttributes.Response.SerializeToString,
      ),
      'deleteDatasetVersionAttributes': grpc.unary_unary_rpc_method_handler(
          servicer.deleteDatasetVersionAttributes,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionAttributes.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.DeleteDatasetVersionAttributes.Response.SerializeToString,
      ),
      'setDatasetVersionVisibility': grpc.unary_unary_rpc_method_handler(
          servicer.setDatasetVersionVisibility,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.SetDatasetVersionVisibilty.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.SetDatasetVersionVisibilty.Response.SerializeToString,
      ),
      'getUrlForDatasetBlobVersioned': grpc.unary_unary_rpc_method_handler(
          servicer.getUrlForDatasetBlobVersioned,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.GetUrlForDatasetBlobVersioned.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.GetUrlForDatasetBlobVersioned.Response.SerializeToString,
      ),
      'commitVersionedDatasetBlobArtifactPart': grpc.unary_unary_rpc_method_handler(
          servicer.commitVersionedDatasetBlobArtifactPart,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.CommitVersionedDatasetBlobArtifactPart.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.CommitVersionedDatasetBlobArtifactPart.Response.SerializeToString,
      ),
      'getCommittedVersionedDatasetBlobArtifactParts': grpc.unary_unary_rpc_method_handler(
          servicer.getCommittedVersionedDatasetBlobArtifactParts,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.GetCommittedVersionedDatasetBlobArtifactParts.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.GetCommittedVersionedDatasetBlobArtifactParts.Response.SerializeToString,
      ),
      'commitMultipartVersionedDatasetBlobArtifact': grpc.unary_unary_rpc_method_handler(
          servicer.commitMultipartVersionedDatasetBlobArtifact,
          request_deserializer=modeldb_dot_DatasetVersionService__pb2.CommitMultipartVersionedDatasetBlobArtifact.FromString,
          response_serializer=modeldb_dot_DatasetVersionService__pb2.CommitMultipartVersionedDatasetBlobArtifact.Response.SerializeToString,
      ),
  }
  generic_handler = grpc.method_handlers_generic_handler(
      'ai.verta.modeldb.DatasetVersionService', rpc_method_handlers)
  server.add_generic_rpc_handlers((generic_handler,))
