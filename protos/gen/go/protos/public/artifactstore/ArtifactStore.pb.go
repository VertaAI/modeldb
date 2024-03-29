// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.31.0
// 	protoc        v3.11.2
// source: artifactstore/ArtifactStore.proto

package artifactstore

import (
	context "context"
	_ "google.golang.org/genproto/googleapis/api/annotations"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

type StoreArtifact struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Key  string `protobuf:"bytes,1,opt,name=key,proto3" json:"key,omitempty"`
	Path string `protobuf:"bytes,2,opt,name=path,proto3" json:"path,omitempty"`
}

func (x *StoreArtifact) Reset() {
	*x = StoreArtifact{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *StoreArtifact) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*StoreArtifact) ProtoMessage() {}

func (x *StoreArtifact) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use StoreArtifact.ProtoReflect.Descriptor instead.
func (*StoreArtifact) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{0}
}

func (x *StoreArtifact) GetKey() string {
	if x != nil {
		return x.Key
	}
	return ""
}

func (x *StoreArtifact) GetPath() string {
	if x != nil {
		return x.Path
	}
	return ""
}

type StoreArtifactWithStream struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Key        string `protobuf:"bytes,1,opt,name=key,proto3" json:"key,omitempty"`
	ClientFile []byte `protobuf:"bytes,2,opt,name=client_file,json=clientFile,proto3" json:"client_file,omitempty"`
}

func (x *StoreArtifactWithStream) Reset() {
	*x = StoreArtifactWithStream{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *StoreArtifactWithStream) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*StoreArtifactWithStream) ProtoMessage() {}

func (x *StoreArtifactWithStream) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use StoreArtifactWithStream.ProtoReflect.Descriptor instead.
func (*StoreArtifactWithStream) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{1}
}

func (x *StoreArtifactWithStream) GetKey() string {
	if x != nil {
		return x.Key
	}
	return ""
}

func (x *StoreArtifactWithStream) GetClientFile() []byte {
	if x != nil {
		return x.ClientFile
	}
	return nil
}

type GetArtifact struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Key string `protobuf:"bytes,1,opt,name=key,proto3" json:"key,omitempty"`
}

func (x *GetArtifact) Reset() {
	*x = GetArtifact{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetArtifact) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetArtifact) ProtoMessage() {}

func (x *GetArtifact) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetArtifact.ProtoReflect.Descriptor instead.
func (*GetArtifact) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{2}
}

func (x *GetArtifact) GetKey() string {
	if x != nil {
		return x.Key
	}
	return ""
}

type DeleteArtifact struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Key string `protobuf:"bytes,1,opt,name=key,proto3" json:"key,omitempty"`
}

func (x *DeleteArtifact) Reset() {
	*x = DeleteArtifact{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *DeleteArtifact) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*DeleteArtifact) ProtoMessage() {}

func (x *DeleteArtifact) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use DeleteArtifact.ProtoReflect.Descriptor instead.
func (*DeleteArtifact) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{3}
}

func (x *DeleteArtifact) GetKey() string {
	if x != nil {
		return x.Key
	}
	return ""
}

type StoreArtifact_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	ArtifactStoreKey  string `protobuf:"bytes,1,opt,name=artifact_store_key,json=artifactStoreKey,proto3" json:"artifact_store_key,omitempty"`
	ArtifactStorePath string `protobuf:"bytes,2,opt,name=artifact_store_path,json=artifactStorePath,proto3" json:"artifact_store_path,omitempty"`
}

func (x *StoreArtifact_Response) Reset() {
	*x = StoreArtifact_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[4]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *StoreArtifact_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*StoreArtifact_Response) ProtoMessage() {}

func (x *StoreArtifact_Response) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[4]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use StoreArtifact_Response.ProtoReflect.Descriptor instead.
func (*StoreArtifact_Response) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{0, 0}
}

func (x *StoreArtifact_Response) GetArtifactStoreKey() string {
	if x != nil {
		return x.ArtifactStoreKey
	}
	return ""
}

func (x *StoreArtifact_Response) GetArtifactStorePath() string {
	if x != nil {
		return x.ArtifactStorePath
	}
	return ""
}

type StoreArtifactWithStream_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	CloudFileKey  string `protobuf:"bytes,1,opt,name=cloud_file_key,json=cloudFileKey,proto3" json:"cloud_file_key,omitempty"`
	CloudFilePath string `protobuf:"bytes,2,opt,name=cloud_file_path,json=cloudFilePath,proto3" json:"cloud_file_path,omitempty"`
}

func (x *StoreArtifactWithStream_Response) Reset() {
	*x = StoreArtifactWithStream_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[5]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *StoreArtifactWithStream_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*StoreArtifactWithStream_Response) ProtoMessage() {}

func (x *StoreArtifactWithStream_Response) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[5]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use StoreArtifactWithStream_Response.ProtoReflect.Descriptor instead.
func (*StoreArtifactWithStream_Response) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{1, 0}
}

func (x *StoreArtifactWithStream_Response) GetCloudFileKey() string {
	if x != nil {
		return x.CloudFileKey
	}
	return ""
}

func (x *StoreArtifactWithStream_Response) GetCloudFilePath() string {
	if x != nil {
		return x.CloudFilePath
	}
	return ""
}

type GetArtifact_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Contents []byte `protobuf:"bytes,1,opt,name=contents,proto3" json:"contents,omitempty"`
}

func (x *GetArtifact_Response) Reset() {
	*x = GetArtifact_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[6]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetArtifact_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetArtifact_Response) ProtoMessage() {}

func (x *GetArtifact_Response) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[6]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetArtifact_Response.ProtoReflect.Descriptor instead.
func (*GetArtifact_Response) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{2, 0}
}

func (x *GetArtifact_Response) GetContents() []byte {
	if x != nil {
		return x.Contents
	}
	return nil
}

type DeleteArtifact_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Status bool `protobuf:"varint,1,opt,name=status,proto3" json:"status,omitempty"`
}

func (x *DeleteArtifact_Response) Reset() {
	*x = DeleteArtifact_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_artifactstore_ArtifactStore_proto_msgTypes[7]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *DeleteArtifact_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*DeleteArtifact_Response) ProtoMessage() {}

func (x *DeleteArtifact_Response) ProtoReflect() protoreflect.Message {
	mi := &file_artifactstore_ArtifactStore_proto_msgTypes[7]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use DeleteArtifact_Response.ProtoReflect.Descriptor instead.
func (*DeleteArtifact_Response) Descriptor() ([]byte, []int) {
	return file_artifactstore_ArtifactStore_proto_rawDescGZIP(), []int{3, 0}
}

func (x *DeleteArtifact_Response) GetStatus() bool {
	if x != nil {
		return x.Status
	}
	return false
}

var File_artifactstore_ArtifactStore_proto protoreflect.FileDescriptor

var file_artifactstore_ArtifactStore_proto_rawDesc = []byte{
	0x0a, 0x21, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2f,
	0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x53, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x70, 0x72,
	0x6f, 0x74, 0x6f, 0x12, 0x16, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x61, 0x72,
	0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x1a, 0x1c, 0x67, 0x6f, 0x6f,
	0x67, 0x6c, 0x65, 0x2f, 0x61, 0x70, 0x69, 0x2f, 0x61, 0x6e, 0x6e, 0x6f, 0x74, 0x61, 0x74, 0x69,
	0x6f, 0x6e, 0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0x9f, 0x01, 0x0a, 0x0d, 0x53, 0x74,
	0x6f, 0x72, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x12, 0x10, 0x0a, 0x03, 0x6b,
	0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x03, 0x6b, 0x65, 0x79, 0x12, 0x12, 0x0a,
	0x04, 0x70, 0x61, 0x74, 0x68, 0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04, 0x70, 0x61, 0x74,
	0x68, 0x1a, 0x68, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x2c, 0x0a,
	0x12, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x5f, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x5f,
	0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x10, 0x61, 0x72, 0x74, 0x69, 0x66,
	0x61, 0x63, 0x74, 0x53, 0x74, 0x6f, 0x72, 0x65, 0x4b, 0x65, 0x79, 0x12, 0x2e, 0x0a, 0x13, 0x61,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x5f, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x5f, 0x70, 0x61,
	0x74, 0x68, 0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x11, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61,
	0x63, 0x74, 0x53, 0x74, 0x6f, 0x72, 0x65, 0x50, 0x61, 0x74, 0x68, 0x22, 0xa6, 0x01, 0x0a, 0x17,
	0x53, 0x74, 0x6f, 0x72, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x57, 0x69, 0x74,
	0x68, 0x53, 0x74, 0x72, 0x65, 0x61, 0x6d, 0x12, 0x10, 0x0a, 0x03, 0x6b, 0x65, 0x79, 0x18, 0x01,
	0x20, 0x01, 0x28, 0x09, 0x52, 0x03, 0x6b, 0x65, 0x79, 0x12, 0x1f, 0x0a, 0x0b, 0x63, 0x6c, 0x69,
	0x65, 0x6e, 0x74, 0x5f, 0x66, 0x69, 0x6c, 0x65, 0x18, 0x02, 0x20, 0x01, 0x28, 0x0c, 0x52, 0x0a,
	0x63, 0x6c, 0x69, 0x65, 0x6e, 0x74, 0x46, 0x69, 0x6c, 0x65, 0x1a, 0x58, 0x0a, 0x08, 0x52, 0x65,
	0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x24, 0x0a, 0x0e, 0x63, 0x6c, 0x6f, 0x75, 0x64, 0x5f,
	0x66, 0x69, 0x6c, 0x65, 0x5f, 0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0c,
	0x63, 0x6c, 0x6f, 0x75, 0x64, 0x46, 0x69, 0x6c, 0x65, 0x4b, 0x65, 0x79, 0x12, 0x26, 0x0a, 0x0f,
	0x63, 0x6c, 0x6f, 0x75, 0x64, 0x5f, 0x66, 0x69, 0x6c, 0x65, 0x5f, 0x70, 0x61, 0x74, 0x68, 0x18,
	0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0d, 0x63, 0x6c, 0x6f, 0x75, 0x64, 0x46, 0x69, 0x6c, 0x65,
	0x50, 0x61, 0x74, 0x68, 0x22, 0x47, 0x0a, 0x0b, 0x47, 0x65, 0x74, 0x41, 0x72, 0x74, 0x69, 0x66,
	0x61, 0x63, 0x74, 0x12, 0x10, 0x0a, 0x03, 0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09,
	0x52, 0x03, 0x6b, 0x65, 0x79, 0x1a, 0x26, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73,
	0x65, 0x12, 0x1a, 0x0a, 0x08, 0x63, 0x6f, 0x6e, 0x74, 0x65, 0x6e, 0x74, 0x73, 0x18, 0x01, 0x20,
	0x01, 0x28, 0x0c, 0x52, 0x08, 0x63, 0x6f, 0x6e, 0x74, 0x65, 0x6e, 0x74, 0x73, 0x22, 0x46, 0x0a,
	0x0e, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x12,
	0x10, 0x0a, 0x03, 0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x03, 0x6b, 0x65,
	0x79, 0x1a, 0x22, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x16, 0x0a,
	0x06, 0x73, 0x74, 0x61, 0x74, 0x75, 0x73, 0x18, 0x01, 0x20, 0x01, 0x28, 0x08, 0x52, 0x06, 0x73,
	0x74, 0x61, 0x74, 0x75, 0x73, 0x32, 0xf0, 0x04, 0x0a, 0x0d, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61,
	0x63, 0x74, 0x53, 0x74, 0x6f, 0x72, 0x65, 0x12, 0x8d, 0x01, 0x0a, 0x0d, 0x73, 0x74, 0x6f, 0x72,
	0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x12, 0x25, 0x2e, 0x61, 0x69, 0x2e, 0x76,
	0x65, 0x72, 0x74, 0x61, 0x2e, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f,
	0x72, 0x65, 0x2e, 0x53, 0x74, 0x6f, 0x72, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74,
	0x1a, 0x2e, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x61, 0x72, 0x74, 0x69,
	0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x53, 0x74, 0x6f, 0x72, 0x65, 0x41,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65,
	0x22, 0x25, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x1f, 0x3a, 0x01, 0x2a, 0x22, 0x1a, 0x2f, 0x76, 0x31,
	0x2f, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2f, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x41,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x12, 0xb5, 0x01, 0x0a, 0x17, 0x73, 0x74, 0x6f, 0x72,
	0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x57, 0x69, 0x74, 0x68, 0x53, 0x74, 0x72,
	0x65, 0x61, 0x6d, 0x12, 0x2f, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x61,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x53, 0x74, 0x6f,
	0x72, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x57, 0x69, 0x74, 0x68, 0x53, 0x74,
	0x72, 0x65, 0x61, 0x6d, 0x1a, 0x38, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e,
	0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x53, 0x74,
	0x6f, 0x72, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x57, 0x69, 0x74, 0x68, 0x53,
	0x74, 0x72, 0x65, 0x61, 0x6d, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x2f,
	0x82, 0xd3, 0xe4, 0x93, 0x02, 0x29, 0x3a, 0x01, 0x2a, 0x22, 0x24, 0x2f, 0x76, 0x31, 0x2f, 0x61,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2f, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x41, 0x72, 0x74,
	0x69, 0x66, 0x61, 0x63, 0x74, 0x57, 0x69, 0x74, 0x68, 0x53, 0x74, 0x72, 0x65, 0x61, 0x6d, 0x12,
	0x82, 0x01, 0x0a, 0x0b, 0x67, 0x65, 0x74, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x12,
	0x23, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x61, 0x72, 0x74, 0x69, 0x66,
	0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x47, 0x65, 0x74, 0x41, 0x72, 0x74, 0x69,
	0x66, 0x61, 0x63, 0x74, 0x1a, 0x2c, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e,
	0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x47, 0x65,
	0x74, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e,
	0x73, 0x65, 0x22, 0x20, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x1a, 0x12, 0x18, 0x2f, 0x76, 0x31, 0x2f,
	0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2f, 0x67, 0x65, 0x74, 0x41, 0x72, 0x74, 0x69,
	0x66, 0x61, 0x63, 0x74, 0x12, 0x91, 0x01, 0x0a, 0x0e, 0x64, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x41,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x12, 0x26, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65,
	0x2e, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x1a,
	0x2f, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x61, 0x72, 0x74, 0x69, 0x66,
	0x61, 0x63, 0x74, 0x73, 0x74, 0x6f, 0x72, 0x65, 0x2e, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x41,
	0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65,
	0x22, 0x26, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x20, 0x3a, 0x01, 0x2a, 0x22, 0x1b, 0x2f, 0x76, 0x31,
	0x2f, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x2f, 0x64, 0x65, 0x6c, 0x65, 0x74, 0x65,
	0x41, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x42, 0x48, 0x50, 0x01, 0x5a, 0x44, 0x67, 0x69,
	0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x56, 0x65, 0x72, 0x74, 0x61, 0x41, 0x49,
	0x2f, 0x6d, 0x6f, 0x64, 0x65, 0x6c, 0x64, 0x62, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f,
	0x67, 0x65, 0x6e, 0x2f, 0x67, 0x6f, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f, 0x70, 0x75,
	0x62, 0x6c, 0x69, 0x63, 0x2f, 0x61, 0x72, 0x74, 0x69, 0x66, 0x61, 0x63, 0x74, 0x73, 0x74, 0x6f,
	0x72, 0x65, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_artifactstore_ArtifactStore_proto_rawDescOnce sync.Once
	file_artifactstore_ArtifactStore_proto_rawDescData = file_artifactstore_ArtifactStore_proto_rawDesc
)

func file_artifactstore_ArtifactStore_proto_rawDescGZIP() []byte {
	file_artifactstore_ArtifactStore_proto_rawDescOnce.Do(func() {
		file_artifactstore_ArtifactStore_proto_rawDescData = protoimpl.X.CompressGZIP(file_artifactstore_ArtifactStore_proto_rawDescData)
	})
	return file_artifactstore_ArtifactStore_proto_rawDescData
}

var file_artifactstore_ArtifactStore_proto_msgTypes = make([]protoimpl.MessageInfo, 8)
var file_artifactstore_ArtifactStore_proto_goTypes = []interface{}{
	(*StoreArtifact)(nil),                    // 0: ai.verta.artifactstore.StoreArtifact
	(*StoreArtifactWithStream)(nil),          // 1: ai.verta.artifactstore.StoreArtifactWithStream
	(*GetArtifact)(nil),                      // 2: ai.verta.artifactstore.GetArtifact
	(*DeleteArtifact)(nil),                   // 3: ai.verta.artifactstore.DeleteArtifact
	(*StoreArtifact_Response)(nil),           // 4: ai.verta.artifactstore.StoreArtifact.Response
	(*StoreArtifactWithStream_Response)(nil), // 5: ai.verta.artifactstore.StoreArtifactWithStream.Response
	(*GetArtifact_Response)(nil),             // 6: ai.verta.artifactstore.GetArtifact.Response
	(*DeleteArtifact_Response)(nil),          // 7: ai.verta.artifactstore.DeleteArtifact.Response
}
var file_artifactstore_ArtifactStore_proto_depIdxs = []int32{
	0, // 0: ai.verta.artifactstore.ArtifactStore.storeArtifact:input_type -> ai.verta.artifactstore.StoreArtifact
	1, // 1: ai.verta.artifactstore.ArtifactStore.storeArtifactWithStream:input_type -> ai.verta.artifactstore.StoreArtifactWithStream
	2, // 2: ai.verta.artifactstore.ArtifactStore.getArtifact:input_type -> ai.verta.artifactstore.GetArtifact
	3, // 3: ai.verta.artifactstore.ArtifactStore.deleteArtifact:input_type -> ai.verta.artifactstore.DeleteArtifact
	4, // 4: ai.verta.artifactstore.ArtifactStore.storeArtifact:output_type -> ai.verta.artifactstore.StoreArtifact.Response
	5, // 5: ai.verta.artifactstore.ArtifactStore.storeArtifactWithStream:output_type -> ai.verta.artifactstore.StoreArtifactWithStream.Response
	6, // 6: ai.verta.artifactstore.ArtifactStore.getArtifact:output_type -> ai.verta.artifactstore.GetArtifact.Response
	7, // 7: ai.verta.artifactstore.ArtifactStore.deleteArtifact:output_type -> ai.verta.artifactstore.DeleteArtifact.Response
	4, // [4:8] is the sub-list for method output_type
	0, // [0:4] is the sub-list for method input_type
	0, // [0:0] is the sub-list for extension type_name
	0, // [0:0] is the sub-list for extension extendee
	0, // [0:0] is the sub-list for field type_name
}

func init() { file_artifactstore_ArtifactStore_proto_init() }
func file_artifactstore_ArtifactStore_proto_init() {
	if File_artifactstore_ArtifactStore_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_artifactstore_ArtifactStore_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*StoreArtifact); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*StoreArtifactWithStream); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetArtifact); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*DeleteArtifact); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[4].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*StoreArtifact_Response); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[5].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*StoreArtifactWithStream_Response); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[6].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetArtifact_Response); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_artifactstore_ArtifactStore_proto_msgTypes[7].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*DeleteArtifact_Response); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_artifactstore_ArtifactStore_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   8,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_artifactstore_ArtifactStore_proto_goTypes,
		DependencyIndexes: file_artifactstore_ArtifactStore_proto_depIdxs,
		MessageInfos:      file_artifactstore_ArtifactStore_proto_msgTypes,
	}.Build()
	File_artifactstore_ArtifactStore_proto = out.File
	file_artifactstore_ArtifactStore_proto_rawDesc = nil
	file_artifactstore_ArtifactStore_proto_goTypes = nil
	file_artifactstore_ArtifactStore_proto_depIdxs = nil
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConnInterface

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// ArtifactStoreClient is the client API for ArtifactStore service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type ArtifactStoreClient interface {
	StoreArtifact(ctx context.Context, in *StoreArtifact, opts ...grpc.CallOption) (*StoreArtifact_Response, error)
	StoreArtifactWithStream(ctx context.Context, in *StoreArtifactWithStream, opts ...grpc.CallOption) (*StoreArtifactWithStream_Response, error)
	GetArtifact(ctx context.Context, in *GetArtifact, opts ...grpc.CallOption) (*GetArtifact_Response, error)
	DeleteArtifact(ctx context.Context, in *DeleteArtifact, opts ...grpc.CallOption) (*DeleteArtifact_Response, error)
}

type artifactStoreClient struct {
	cc grpc.ClientConnInterface
}

func NewArtifactStoreClient(cc grpc.ClientConnInterface) ArtifactStoreClient {
	return &artifactStoreClient{cc}
}

func (c *artifactStoreClient) StoreArtifact(ctx context.Context, in *StoreArtifact, opts ...grpc.CallOption) (*StoreArtifact_Response, error) {
	out := new(StoreArtifact_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.artifactstore.ArtifactStore/storeArtifact", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *artifactStoreClient) StoreArtifactWithStream(ctx context.Context, in *StoreArtifactWithStream, opts ...grpc.CallOption) (*StoreArtifactWithStream_Response, error) {
	out := new(StoreArtifactWithStream_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.artifactstore.ArtifactStore/storeArtifactWithStream", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *artifactStoreClient) GetArtifact(ctx context.Context, in *GetArtifact, opts ...grpc.CallOption) (*GetArtifact_Response, error) {
	out := new(GetArtifact_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.artifactstore.ArtifactStore/getArtifact", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *artifactStoreClient) DeleteArtifact(ctx context.Context, in *DeleteArtifact, opts ...grpc.CallOption) (*DeleteArtifact_Response, error) {
	out := new(DeleteArtifact_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.artifactstore.ArtifactStore/deleteArtifact", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// ArtifactStoreServer is the server API for ArtifactStore service.
type ArtifactStoreServer interface {
	StoreArtifact(context.Context, *StoreArtifact) (*StoreArtifact_Response, error)
	StoreArtifactWithStream(context.Context, *StoreArtifactWithStream) (*StoreArtifactWithStream_Response, error)
	GetArtifact(context.Context, *GetArtifact) (*GetArtifact_Response, error)
	DeleteArtifact(context.Context, *DeleteArtifact) (*DeleteArtifact_Response, error)
}

// UnimplementedArtifactStoreServer can be embedded to have forward compatible implementations.
type UnimplementedArtifactStoreServer struct {
}

func (*UnimplementedArtifactStoreServer) StoreArtifact(context.Context, *StoreArtifact) (*StoreArtifact_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method StoreArtifact not implemented")
}
func (*UnimplementedArtifactStoreServer) StoreArtifactWithStream(context.Context, *StoreArtifactWithStream) (*StoreArtifactWithStream_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method StoreArtifactWithStream not implemented")
}
func (*UnimplementedArtifactStoreServer) GetArtifact(context.Context, *GetArtifact) (*GetArtifact_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetArtifact not implemented")
}
func (*UnimplementedArtifactStoreServer) DeleteArtifact(context.Context, *DeleteArtifact) (*DeleteArtifact_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method DeleteArtifact not implemented")
}

func RegisterArtifactStoreServer(s *grpc.Server, srv ArtifactStoreServer) {
	s.RegisterService(&_ArtifactStore_serviceDesc, srv)
}

func _ArtifactStore_StoreArtifact_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(StoreArtifact)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(ArtifactStoreServer).StoreArtifact(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.artifactstore.ArtifactStore/StoreArtifact",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(ArtifactStoreServer).StoreArtifact(ctx, req.(*StoreArtifact))
	}
	return interceptor(ctx, in, info, handler)
}

func _ArtifactStore_StoreArtifactWithStream_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(StoreArtifactWithStream)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(ArtifactStoreServer).StoreArtifactWithStream(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.artifactstore.ArtifactStore/StoreArtifactWithStream",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(ArtifactStoreServer).StoreArtifactWithStream(ctx, req.(*StoreArtifactWithStream))
	}
	return interceptor(ctx, in, info, handler)
}

func _ArtifactStore_GetArtifact_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(GetArtifact)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(ArtifactStoreServer).GetArtifact(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.artifactstore.ArtifactStore/GetArtifact",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(ArtifactStoreServer).GetArtifact(ctx, req.(*GetArtifact))
	}
	return interceptor(ctx, in, info, handler)
}

func _ArtifactStore_DeleteArtifact_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(DeleteArtifact)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(ArtifactStoreServer).DeleteArtifact(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.artifactstore.ArtifactStore/DeleteArtifact",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(ArtifactStoreServer).DeleteArtifact(ctx, req.(*DeleteArtifact))
	}
	return interceptor(ctx, in, info, handler)
}

var _ArtifactStore_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ai.verta.artifactstore.ArtifactStore",
	HandlerType: (*ArtifactStoreServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "storeArtifact",
			Handler:    _ArtifactStore_StoreArtifact_Handler,
		},
		{
			MethodName: "storeArtifactWithStream",
			Handler:    _ArtifactStore_StoreArtifactWithStream_Handler,
		},
		{
			MethodName: "getArtifact",
			Handler:    _ArtifactStore_GetArtifact_Handler,
		},
		{
			MethodName: "deleteArtifact",
			Handler:    _ArtifactStore_DeleteArtifact_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "artifactstore/ArtifactStore.proto",
}
