// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.31.0
// 	protoc        v3.11.2
// source: uac/ServiceAccount.proto

package uac

import (
	common "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	_ "google.golang.org/genproto/googleapis/api/annotations"
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

// A service account is a subtype of User. So we can use the UAC APIs to manipulate the service account.
// Besides the service account itself, the organization admin should be able to get and manipulate the user details, like dev keys.
type ServiceAccount struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Id     uint64 `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`
	UserId string `protobuf:"bytes,2,opt,name=user_id,json=userId,proto3" json:"user_id,omitempty"` // This is the verta user id that we use all across the system
	// A service account belongs to a fixed workspace
	AssociatedWorkspaceId uint64 `protobuf:"varint,3,opt,name=associated_workspace_id,json=associatedWorkspaceId,proto3" json:"associated_workspace_id,omitempty"`
	CreationTimestamp     uint64 `protobuf:"varint,4,opt,name=creation_timestamp,json=creationTimestamp,proto3" json:"creation_timestamp,omitempty"`
	Description           string `protobuf:"bytes,5,opt,name=description,proto3" json:"description,omitempty"`
}

func (x *ServiceAccount) Reset() {
	*x = ServiceAccount{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_ServiceAccount_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *ServiceAccount) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*ServiceAccount) ProtoMessage() {}

func (x *ServiceAccount) ProtoReflect() protoreflect.Message {
	mi := &file_uac_ServiceAccount_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use ServiceAccount.ProtoReflect.Descriptor instead.
func (*ServiceAccount) Descriptor() ([]byte, []int) {
	return file_uac_ServiceAccount_proto_rawDescGZIP(), []int{0}
}

func (x *ServiceAccount) GetId() uint64 {
	if x != nil {
		return x.Id
	}
	return 0
}

func (x *ServiceAccount) GetUserId() string {
	if x != nil {
		return x.UserId
	}
	return ""
}

func (x *ServiceAccount) GetAssociatedWorkspaceId() uint64 {
	if x != nil {
		return x.AssociatedWorkspaceId
	}
	return 0
}

func (x *ServiceAccount) GetCreationTimestamp() uint64 {
	if x != nil {
		return x.CreationTimestamp
	}
	return 0
}

func (x *ServiceAccount) GetDescription() string {
	if x != nil {
		return x.Description
	}
	return ""
}

// Only the org admins can create a service account. This is controlled by RBAC.
type CreateServiceAccountRequest struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	// The final username for the service account will be "workspace_name/username". We should prevent usernames that have a "/" otherwise.
	Username              string `protobuf:"bytes,1,opt,name=username,proto3" json:"username,omitempty"`
	AssociatedWorkspaceId uint64 `protobuf:"varint,2,opt,name=associated_workspace_id,json=associatedWorkspaceId,proto3" json:"associated_workspace_id,omitempty"`
	Description           string `protobuf:"bytes,4,opt,name=description,proto3" json:"description,omitempty"`
}

func (x *CreateServiceAccountRequest) Reset() {
	*x = CreateServiceAccountRequest{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_ServiceAccount_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *CreateServiceAccountRequest) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*CreateServiceAccountRequest) ProtoMessage() {}

func (x *CreateServiceAccountRequest) ProtoReflect() protoreflect.Message {
	mi := &file_uac_ServiceAccount_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use CreateServiceAccountRequest.ProtoReflect.Descriptor instead.
func (*CreateServiceAccountRequest) Descriptor() ([]byte, []int) {
	return file_uac_ServiceAccount_proto_rawDescGZIP(), []int{1}
}

func (x *CreateServiceAccountRequest) GetUsername() string {
	if x != nil {
		return x.Username
	}
	return ""
}

func (x *CreateServiceAccountRequest) GetAssociatedWorkspaceId() uint64 {
	if x != nil {
		return x.AssociatedWorkspaceId
	}
	return 0
}

func (x *CreateServiceAccountRequest) GetDescription() string {
	if x != nil {
		return x.Description
	}
	return ""
}

type UpdateServiceAccountRequest struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Id          uint64 `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`
	Description string `protobuf:"bytes,4,opt,name=description,proto3" json:"description,omitempty"`
}

func (x *UpdateServiceAccountRequest) Reset() {
	*x = UpdateServiceAccountRequest{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_ServiceAccount_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *UpdateServiceAccountRequest) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*UpdateServiceAccountRequest) ProtoMessage() {}

func (x *UpdateServiceAccountRequest) ProtoReflect() protoreflect.Message {
	mi := &file_uac_ServiceAccount_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use UpdateServiceAccountRequest.ProtoReflect.Descriptor instead.
func (*UpdateServiceAccountRequest) Descriptor() ([]byte, []int) {
	return file_uac_ServiceAccount_proto_rawDescGZIP(), []int{2}
}

func (x *UpdateServiceAccountRequest) GetId() uint64 {
	if x != nil {
		return x.Id
	}
	return 0
}

func (x *UpdateServiceAccountRequest) GetDescription() string {
	if x != nil {
		return x.Description
	}
	return ""
}

// Only the org admins can delete a service account. This is controlled by RBAC.
type DeleteServiceAccountRequest struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Ids []uint64 `protobuf:"varint,1,rep,packed,name=ids,proto3" json:"ids,omitempty"`
}

func (x *DeleteServiceAccountRequest) Reset() {
	*x = DeleteServiceAccountRequest{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_ServiceAccount_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *DeleteServiceAccountRequest) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*DeleteServiceAccountRequest) ProtoMessage() {}

func (x *DeleteServiceAccountRequest) ProtoReflect() protoreflect.Message {
	mi := &file_uac_ServiceAccount_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use DeleteServiceAccountRequest.ProtoReflect.Descriptor instead.
func (*DeleteServiceAccountRequest) Descriptor() ([]byte, []int) {
	return file_uac_ServiceAccount_proto_rawDescGZIP(), []int{3}
}

func (x *DeleteServiceAccountRequest) GetIds() []uint64 {
	if x != nil {
		return x.Ids
	}
	return nil
}

// Perform the query computing an AND of all the fields that are present, with IN operator for the list items.
// The user must be authorized to READ all IDs it's requesting.
// A user can READ a service account if they belong to the same workspace.
type FindServiceAccountRequest struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Ids                    []uint64           `protobuf:"varint,1,rep,packed,name=ids,proto3" json:"ids,omitempty"`
	AssociatedWorkspaceIds []uint64           `protobuf:"varint,2,rep,packed,name=associated_workspace_ids,json=associatedWorkspaceIds,proto3" json:"associated_workspace_ids,omitempty"`
	Usernames              []string           `protobuf:"bytes,3,rep,name=usernames,proto3" json:"usernames,omitempty"`
	Pagination             *common.Pagination `protobuf:"bytes,4,opt,name=pagination,proto3" json:"pagination,omitempty"`
}

func (x *FindServiceAccountRequest) Reset() {
	*x = FindServiceAccountRequest{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_ServiceAccount_proto_msgTypes[4]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *FindServiceAccountRequest) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*FindServiceAccountRequest) ProtoMessage() {}

func (x *FindServiceAccountRequest) ProtoReflect() protoreflect.Message {
	mi := &file_uac_ServiceAccount_proto_msgTypes[4]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use FindServiceAccountRequest.ProtoReflect.Descriptor instead.
func (*FindServiceAccountRequest) Descriptor() ([]byte, []int) {
	return file_uac_ServiceAccount_proto_rawDescGZIP(), []int{4}
}

func (x *FindServiceAccountRequest) GetIds() []uint64 {
	if x != nil {
		return x.Ids
	}
	return nil
}

func (x *FindServiceAccountRequest) GetAssociatedWorkspaceIds() []uint64 {
	if x != nil {
		return x.AssociatedWorkspaceIds
	}
	return nil
}

func (x *FindServiceAccountRequest) GetUsernames() []string {
	if x != nil {
		return x.Usernames
	}
	return nil
}

func (x *FindServiceAccountRequest) GetPagination() *common.Pagination {
	if x != nil {
		return x.Pagination
	}
	return nil
}

type FindServiceAccountRequest_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	ServiceAccounts []*ServiceAccount `protobuf:"bytes,1,rep,name=service_accounts,json=serviceAccounts,proto3" json:"service_accounts,omitempty"`
	TotalRecords    int64             `protobuf:"varint,2,opt,name=total_records,json=totalRecords,proto3" json:"total_records,omitempty"`
}

func (x *FindServiceAccountRequest_Response) Reset() {
	*x = FindServiceAccountRequest_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_ServiceAccount_proto_msgTypes[5]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *FindServiceAccountRequest_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*FindServiceAccountRequest_Response) ProtoMessage() {}

func (x *FindServiceAccountRequest_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_ServiceAccount_proto_msgTypes[5]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use FindServiceAccountRequest_Response.ProtoReflect.Descriptor instead.
func (*FindServiceAccountRequest_Response) Descriptor() ([]byte, []int) {
	return file_uac_ServiceAccount_proto_rawDescGZIP(), []int{4, 0}
}

func (x *FindServiceAccountRequest_Response) GetServiceAccounts() []*ServiceAccount {
	if x != nil {
		return x.ServiceAccounts
	}
	return nil
}

func (x *FindServiceAccountRequest_Response) GetTotalRecords() int64 {
	if x != nil {
		return x.TotalRecords
	}
	return 0
}

var File_uac_ServiceAccount_proto protoreflect.FileDescriptor

var file_uac_ServiceAccount_proto_rawDesc = []byte{
	0x0a, 0x18, 0x75, 0x61, 0x63, 0x2f, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63,
	0x6f, 0x75, 0x6e, 0x74, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x0c, 0x61, 0x69, 0x2e, 0x76,
	0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x1a, 0x1c, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65,
	0x2f, 0x61, 0x70, 0x69, 0x2f, 0x61, 0x6e, 0x6e, 0x6f, 0x74, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x73,
	0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x1a, 0x14, 0x75, 0x61, 0x63, 0x2f, 0x55, 0x41, 0x43, 0x53,
	0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x1a, 0x1a, 0x63, 0x6f,
	0x6d, 0x6d, 0x6f, 0x6e, 0x2f, 0x43, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x53, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0xc2, 0x01, 0x0a, 0x0e, 0x53, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x12, 0x0e, 0x0a, 0x02, 0x69,
	0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x04, 0x52, 0x02, 0x69, 0x64, 0x12, 0x17, 0x0a, 0x07, 0x75,
	0x73, 0x65, 0x72, 0x5f, 0x69, 0x64, 0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x06, 0x75, 0x73,
	0x65, 0x72, 0x49, 0x64, 0x12, 0x36, 0x0a, 0x17, 0x61, 0x73, 0x73, 0x6f, 0x63, 0x69, 0x61, 0x74,
	0x65, 0x64, 0x5f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x5f, 0x69, 0x64, 0x18,
	0x03, 0x20, 0x01, 0x28, 0x04, 0x52, 0x15, 0x61, 0x73, 0x73, 0x6f, 0x63, 0x69, 0x61, 0x74, 0x65,
	0x64, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x49, 0x64, 0x12, 0x2d, 0x0a, 0x12,
	0x63, 0x72, 0x65, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x5f, 0x74, 0x69, 0x6d, 0x65, 0x73, 0x74, 0x61,
	0x6d, 0x70, 0x18, 0x04, 0x20, 0x01, 0x28, 0x04, 0x52, 0x11, 0x63, 0x72, 0x65, 0x61, 0x74, 0x69,
	0x6f, 0x6e, 0x54, 0x69, 0x6d, 0x65, 0x73, 0x74, 0x61, 0x6d, 0x70, 0x12, 0x20, 0x0a, 0x0b, 0x64,
	0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x18, 0x05, 0x20, 0x01, 0x28, 0x09,
	0x52, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x22, 0x93, 0x01,
	0x0a, 0x1b, 0x43, 0x72, 0x65, 0x61, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41,
	0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x12, 0x1a, 0x0a,
	0x08, 0x75, 0x73, 0x65, 0x72, 0x6e, 0x61, 0x6d, 0x65, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52,
	0x08, 0x75, 0x73, 0x65, 0x72, 0x6e, 0x61, 0x6d, 0x65, 0x12, 0x36, 0x0a, 0x17, 0x61, 0x73, 0x73,
	0x6f, 0x63, 0x69, 0x61, 0x74, 0x65, 0x64, 0x5f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63,
	0x65, 0x5f, 0x69, 0x64, 0x18, 0x02, 0x20, 0x01, 0x28, 0x04, 0x52, 0x15, 0x61, 0x73, 0x73, 0x6f,
	0x63, 0x69, 0x61, 0x74, 0x65, 0x64, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x49,
	0x64, 0x12, 0x20, 0x0a, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e,
	0x18, 0x04, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74,
	0x69, 0x6f, 0x6e, 0x22, 0x4f, 0x0a, 0x1b, 0x55, 0x70, 0x64, 0x61, 0x74, 0x65, 0x53, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65,
	0x73, 0x74, 0x12, 0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x04, 0x52, 0x02,
	0x69, 0x64, 0x12, 0x20, 0x0a, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f,
	0x6e, 0x18, 0x04, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70,
	0x74, 0x69, 0x6f, 0x6e, 0x22, 0x2f, 0x0a, 0x1b, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x53, 0x65,
	0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75,
	0x65, 0x73, 0x74, 0x12, 0x10, 0x0a, 0x03, 0x69, 0x64, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x04,
	0x52, 0x03, 0x69, 0x64, 0x73, 0x22, 0xbc, 0x02, 0x0a, 0x19, 0x46, 0x69, 0x6e, 0x64, 0x53, 0x65,
	0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75,
	0x65, 0x73, 0x74, 0x12, 0x10, 0x0a, 0x03, 0x69, 0x64, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x04,
	0x52, 0x03, 0x69, 0x64, 0x73, 0x12, 0x38, 0x0a, 0x18, 0x61, 0x73, 0x73, 0x6f, 0x63, 0x69, 0x61,
	0x74, 0x65, 0x64, 0x5f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x5f, 0x69, 0x64,
	0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x04, 0x52, 0x16, 0x61, 0x73, 0x73, 0x6f, 0x63, 0x69, 0x61,
	0x74, 0x65, 0x64, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x49, 0x64, 0x73, 0x12,
	0x1c, 0x0a, 0x09, 0x75, 0x73, 0x65, 0x72, 0x6e, 0x61, 0x6d, 0x65, 0x73, 0x18, 0x03, 0x20, 0x03,
	0x28, 0x09, 0x52, 0x09, 0x75, 0x73, 0x65, 0x72, 0x6e, 0x61, 0x6d, 0x65, 0x73, 0x12, 0x3b, 0x0a,
	0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x18, 0x04, 0x20, 0x01, 0x28,
	0x0b, 0x32, 0x1b, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x63, 0x6f, 0x6d,
	0x6d, 0x6f, 0x6e, 0x2e, 0x50, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x52, 0x0a,
	0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x1a, 0x78, 0x0a, 0x08, 0x52, 0x65,
	0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x47, 0x0a, 0x10, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63,
	0x65, 0x5f, 0x61, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x0b,
	0x32, 0x1c, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e,
	0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x0f,
	0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x73, 0x12,
	0x23, 0x0a, 0x0d, 0x74, 0x6f, 0x74, 0x61, 0x6c, 0x5f, 0x72, 0x65, 0x63, 0x6f, 0x72, 0x64, 0x73,
	0x18, 0x02, 0x20, 0x01, 0x28, 0x03, 0x52, 0x0c, 0x74, 0x6f, 0x74, 0x61, 0x6c, 0x52, 0x65, 0x63,
	0x6f, 0x72, 0x64, 0x73, 0x32, 0xf8, 0x04, 0x0a, 0x15, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65,
	0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x12, 0x94,
	0x01, 0x0a, 0x14, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65,
	0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x12, 0x29, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x43, 0x72, 0x65, 0x61, 0x74, 0x65, 0x53, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65,
	0x73, 0x74, 0x1a, 0x1c, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61,
	0x63, 0x2e, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74,
	0x22, 0x33, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x2d, 0x3a, 0x01, 0x2a, 0x22, 0x28, 0x2f, 0x76, 0x31,
	0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x5f, 0x61, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74,
	0x2f, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63,
	0x63, 0x6f, 0x75, 0x6e, 0x74, 0x12, 0xa2, 0x01, 0x0a, 0x12, 0x66, 0x69, 0x6e, 0x64, 0x53, 0x65,
	0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x12, 0x27, 0x2e, 0x61,
	0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x46, 0x69, 0x6e, 0x64,
	0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65,
	0x71, 0x75, 0x65, 0x73, 0x74, 0x1a, 0x30, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61,
	0x2e, 0x75, 0x61, 0x63, 0x2e, 0x46, 0x69, 0x6e, 0x64, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65,
	0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x2e, 0x52,
	0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x31, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x2b, 0x3a,
	0x01, 0x2a, 0x22, 0x26, 0x2f, 0x76, 0x31, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x5f,
	0x61, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x2f, 0x66, 0x69, 0x6e, 0x64, 0x53, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x12, 0x8b, 0x01, 0x0a, 0x14, 0x64,
	0x65, 0x6c, 0x65, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f,
	0x75, 0x6e, 0x74, 0x12, 0x29, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75,
	0x61, 0x63, 0x2e, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65,
	0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x1a, 0x13,
	0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x45, 0x6d,
	0x70, 0x74, 0x79, 0x22, 0x33, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x2d, 0x3a, 0x01, 0x2a, 0x2a, 0x28,
	0x2f, 0x76, 0x31, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x5f, 0x61, 0x63, 0x63, 0x6f,
	0x75, 0x6e, 0x74, 0x2f, 0x64, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63,
	0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x12, 0x94, 0x01, 0x0a, 0x14, 0x75, 0x70, 0x64,
	0x61, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e,
	0x74, 0x12, 0x29, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63,
	0x2e, 0x55, 0x70, 0x64, 0x61, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63,
	0x63, 0x6f, 0x75, 0x6e, 0x74, 0x52, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x1a, 0x1c, 0x2e, 0x61,
	0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x53, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x22, 0x33, 0x82, 0xd3, 0xe4, 0x93,
	0x02, 0x2d, 0x3a, 0x01, 0x2a, 0x22, 0x28, 0x2f, 0x76, 0x31, 0x2f, 0x73, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x5f, 0x61, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x2f, 0x75, 0x70, 0x64, 0x61, 0x74,
	0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x41, 0x63, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x42,
	0x3e, 0x50, 0x01, 0x5a, 0x3a, 0x67, 0x69, 0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f,
	0x56, 0x65, 0x72, 0x74, 0x61, 0x41, 0x49, 0x2f, 0x6d, 0x6f, 0x64, 0x65, 0x6c, 0x64, 0x62, 0x2f,
	0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f, 0x67, 0x65, 0x6e, 0x2f, 0x67, 0x6f, 0x2f, 0x70, 0x72,
	0x6f, 0x74, 0x6f, 0x73, 0x2f, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, 0x2f, 0x75, 0x61, 0x63, 0x62,
	0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_uac_ServiceAccount_proto_rawDescOnce sync.Once
	file_uac_ServiceAccount_proto_rawDescData = file_uac_ServiceAccount_proto_rawDesc
)

func file_uac_ServiceAccount_proto_rawDescGZIP() []byte {
	file_uac_ServiceAccount_proto_rawDescOnce.Do(func() {
		file_uac_ServiceAccount_proto_rawDescData = protoimpl.X.CompressGZIP(file_uac_ServiceAccount_proto_rawDescData)
	})
	return file_uac_ServiceAccount_proto_rawDescData
}

var file_uac_ServiceAccount_proto_msgTypes = make([]protoimpl.MessageInfo, 6)
var file_uac_ServiceAccount_proto_goTypes = []interface{}{
	(*ServiceAccount)(nil),                     // 0: ai.verta.uac.ServiceAccount
	(*CreateServiceAccountRequest)(nil),        // 1: ai.verta.uac.CreateServiceAccountRequest
	(*UpdateServiceAccountRequest)(nil),        // 2: ai.verta.uac.UpdateServiceAccountRequest
	(*DeleteServiceAccountRequest)(nil),        // 3: ai.verta.uac.DeleteServiceAccountRequest
	(*FindServiceAccountRequest)(nil),          // 4: ai.verta.uac.FindServiceAccountRequest
	(*FindServiceAccountRequest_Response)(nil), // 5: ai.verta.uac.FindServiceAccountRequest.Response
	(*common.Pagination)(nil),                  // 6: ai.verta.common.Pagination
	(*Empty)(nil),                              // 7: ai.verta.uac.Empty
}
var file_uac_ServiceAccount_proto_depIdxs = []int32{
	6, // 0: ai.verta.uac.FindServiceAccountRequest.pagination:type_name -> ai.verta.common.Pagination
	0, // 1: ai.verta.uac.FindServiceAccountRequest.Response.service_accounts:type_name -> ai.verta.uac.ServiceAccount
	1, // 2: ai.verta.uac.ServiceAccountService.createServiceAccount:input_type -> ai.verta.uac.CreateServiceAccountRequest
	4, // 3: ai.verta.uac.ServiceAccountService.findServiceAccount:input_type -> ai.verta.uac.FindServiceAccountRequest
	3, // 4: ai.verta.uac.ServiceAccountService.deleteServiceAccount:input_type -> ai.verta.uac.DeleteServiceAccountRequest
	2, // 5: ai.verta.uac.ServiceAccountService.updateServiceAccount:input_type -> ai.verta.uac.UpdateServiceAccountRequest
	0, // 6: ai.verta.uac.ServiceAccountService.createServiceAccount:output_type -> ai.verta.uac.ServiceAccount
	5, // 7: ai.verta.uac.ServiceAccountService.findServiceAccount:output_type -> ai.verta.uac.FindServiceAccountRequest.Response
	7, // 8: ai.verta.uac.ServiceAccountService.deleteServiceAccount:output_type -> ai.verta.uac.Empty
	0, // 9: ai.verta.uac.ServiceAccountService.updateServiceAccount:output_type -> ai.verta.uac.ServiceAccount
	6, // [6:10] is the sub-list for method output_type
	2, // [2:6] is the sub-list for method input_type
	2, // [2:2] is the sub-list for extension type_name
	2, // [2:2] is the sub-list for extension extendee
	0, // [0:2] is the sub-list for field type_name
}

func init() { file_uac_ServiceAccount_proto_init() }
func file_uac_ServiceAccount_proto_init() {
	if File_uac_ServiceAccount_proto != nil {
		return
	}
	file_uac_UACService_proto_init()
	if !protoimpl.UnsafeEnabled {
		file_uac_ServiceAccount_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*ServiceAccount); i {
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
		file_uac_ServiceAccount_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*CreateServiceAccountRequest); i {
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
		file_uac_ServiceAccount_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*UpdateServiceAccountRequest); i {
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
		file_uac_ServiceAccount_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*DeleteServiceAccountRequest); i {
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
		file_uac_ServiceAccount_proto_msgTypes[4].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*FindServiceAccountRequest); i {
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
		file_uac_ServiceAccount_proto_msgTypes[5].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*FindServiceAccountRequest_Response); i {
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
			RawDescriptor: file_uac_ServiceAccount_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   6,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_uac_ServiceAccount_proto_goTypes,
		DependencyIndexes: file_uac_ServiceAccount_proto_depIdxs,
		MessageInfos:      file_uac_ServiceAccount_proto_msgTypes,
	}.Build()
	File_uac_ServiceAccount_proto = out.File
	file_uac_ServiceAccount_proto_rawDesc = nil
	file_uac_ServiceAccount_proto_goTypes = nil
	file_uac_ServiceAccount_proto_depIdxs = nil
}
