// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.31.0
// 	protoc        v3.11.2
// source: uac/WorkspaceV2.proto

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

// a Permission is a group + a role, assigned to a workspace
type Permission struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	GroupId string `protobuf:"bytes,2,opt,name=group_id,json=groupId,proto3" json:"group_id,omitempty"`
	RoleId  string `protobuf:"bytes,3,opt,name=role_id,json=roleId,proto3" json:"role_id,omitempty"`
}

func (x *Permission) Reset() {
	*x = Permission{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Permission) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Permission) ProtoMessage() {}

func (x *Permission) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Permission.ProtoReflect.Descriptor instead.
func (*Permission) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{0}
}

func (x *Permission) GetGroupId() string {
	if x != nil {
		return x.GroupId
	}
	return ""
}

func (x *Permission) GetRoleId() string {
	if x != nil {
		return x.RoleId
	}
	return ""
}

type WorkspaceV2 struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	OrgId string `protobuf:"bytes,1,opt,name=org_id,json=orgId,proto3" json:"org_id,omitempty"`
	// compatible type with workspace id in resources proto definition
	Id               uint64        `protobuf:"varint,2,opt,name=id,proto3" json:"id,omitempty"`
	Name             string        `protobuf:"bytes,3,opt,name=name,proto3" json:"name,omitempty"`
	Description      string        `protobuf:"bytes,4,opt,name=description,proto3" json:"description,omitempty"`
	Permissions      []*Permission `protobuf:"bytes,5,rep,name=permissions,proto3" json:"permissions,omitempty"`
	CreatedTimestamp int64         `protobuf:"varint,7,opt,name=created_timestamp,json=createdTimestamp,proto3" json:"created_timestamp,omitempty"`
	UpdatedTimestamp int64         `protobuf:"varint,8,opt,name=updated_timestamp,json=updatedTimestamp,proto3" json:"updated_timestamp,omitempty"`
	Namespace        string        `protobuf:"bytes,9,opt,name=namespace,proto3" json:"namespace,omitempty"`
	BuiltIn          bool          `protobuf:"varint,10,opt,name=built_in,json=builtIn,proto3" json:"built_in,omitempty"`
}

func (x *WorkspaceV2) Reset() {
	*x = WorkspaceV2{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *WorkspaceV2) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*WorkspaceV2) ProtoMessage() {}

func (x *WorkspaceV2) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use WorkspaceV2.ProtoReflect.Descriptor instead.
func (*WorkspaceV2) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{1}
}

func (x *WorkspaceV2) GetOrgId() string {
	if x != nil {
		return x.OrgId
	}
	return ""
}

func (x *WorkspaceV2) GetId() uint64 {
	if x != nil {
		return x.Id
	}
	return 0
}

func (x *WorkspaceV2) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

func (x *WorkspaceV2) GetDescription() string {
	if x != nil {
		return x.Description
	}
	return ""
}

func (x *WorkspaceV2) GetPermissions() []*Permission {
	if x != nil {
		return x.Permissions
	}
	return nil
}

func (x *WorkspaceV2) GetCreatedTimestamp() int64 {
	if x != nil {
		return x.CreatedTimestamp
	}
	return 0
}

func (x *WorkspaceV2) GetUpdatedTimestamp() int64 {
	if x != nil {
		return x.UpdatedTimestamp
	}
	return 0
}

func (x *WorkspaceV2) GetNamespace() string {
	if x != nil {
		return x.Namespace
	}
	return ""
}

func (x *WorkspaceV2) GetBuiltIn() bool {
	if x != nil {
		return x.BuiltIn
	}
	return false
}

type SetWorkspaceV2 struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Workspace *WorkspaceV2 `protobuf:"bytes,2,opt,name=workspace,proto3" json:"workspace,omitempty"`
}

func (x *SetWorkspaceV2) Reset() {
	*x = SetWorkspaceV2{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *SetWorkspaceV2) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*SetWorkspaceV2) ProtoMessage() {}

func (x *SetWorkspaceV2) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use SetWorkspaceV2.ProtoReflect.Descriptor instead.
func (*SetWorkspaceV2) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{2}
}

func (x *SetWorkspaceV2) GetWorkspace() *WorkspaceV2 {
	if x != nil {
		return x.Workspace
	}
	return nil
}

type GetWorkspaceV2 struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	OrgId string `protobuf:"bytes,1,opt,name=org_id,json=orgId,proto3" json:"org_id,omitempty"`
	// Types that are assignable to Identifier:
	//
	//	*GetWorkspaceV2_WorkspaceId
	//	*GetWorkspaceV2_Name
	Identifier isGetWorkspaceV2_Identifier `protobuf_oneof:"identifier"`
}

func (x *GetWorkspaceV2) Reset() {
	*x = GetWorkspaceV2{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetWorkspaceV2) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetWorkspaceV2) ProtoMessage() {}

func (x *GetWorkspaceV2) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetWorkspaceV2.ProtoReflect.Descriptor instead.
func (*GetWorkspaceV2) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{3}
}

func (x *GetWorkspaceV2) GetOrgId() string {
	if x != nil {
		return x.OrgId
	}
	return ""
}

func (m *GetWorkspaceV2) GetIdentifier() isGetWorkspaceV2_Identifier {
	if m != nil {
		return m.Identifier
	}
	return nil
}

func (x *GetWorkspaceV2) GetWorkspaceId() uint64 {
	if x, ok := x.GetIdentifier().(*GetWorkspaceV2_WorkspaceId); ok {
		return x.WorkspaceId
	}
	return 0
}

func (x *GetWorkspaceV2) GetName() string {
	if x, ok := x.GetIdentifier().(*GetWorkspaceV2_Name); ok {
		return x.Name
	}
	return ""
}

type isGetWorkspaceV2_Identifier interface {
	isGetWorkspaceV2_Identifier()
}

type GetWorkspaceV2_WorkspaceId struct {
	WorkspaceId uint64 `protobuf:"varint,2,opt,name=workspace_id,json=workspaceId,proto3,oneof"`
}

type GetWorkspaceV2_Name struct {
	Name string `protobuf:"bytes,3,opt,name=name,proto3,oneof"`
}

func (*GetWorkspaceV2_WorkspaceId) isGetWorkspaceV2_Identifier() {}

func (*GetWorkspaceV2_Name) isGetWorkspaceV2_Identifier() {}

type SearchWorkspacesV2 struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	OrgId      string             `protobuf:"bytes,1,opt,name=org_id,json=orgId,proto3" json:"org_id,omitempty"`
	Pagination *common.Pagination `protobuf:"bytes,2,opt,name=pagination,proto3" json:"pagination,omitempty"`
}

func (x *SearchWorkspacesV2) Reset() {
	*x = SearchWorkspacesV2{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[4]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *SearchWorkspacesV2) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*SearchWorkspacesV2) ProtoMessage() {}

func (x *SearchWorkspacesV2) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[4]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use SearchWorkspacesV2.ProtoReflect.Descriptor instead.
func (*SearchWorkspacesV2) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{4}
}

func (x *SearchWorkspacesV2) GetOrgId() string {
	if x != nil {
		return x.OrgId
	}
	return ""
}

func (x *SearchWorkspacesV2) GetPagination() *common.Pagination {
	if x != nil {
		return x.Pagination
	}
	return nil
}

type DeleteWorkspaceV2 struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	OrgId       string `protobuf:"bytes,1,opt,name=org_id,json=orgId,proto3" json:"org_id,omitempty"`
	WorkspaceId uint64 `protobuf:"varint,2,opt,name=workspace_id,json=workspaceId,proto3" json:"workspace_id,omitempty"`
}

func (x *DeleteWorkspaceV2) Reset() {
	*x = DeleteWorkspaceV2{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[5]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *DeleteWorkspaceV2) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*DeleteWorkspaceV2) ProtoMessage() {}

func (x *DeleteWorkspaceV2) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[5]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use DeleteWorkspaceV2.ProtoReflect.Descriptor instead.
func (*DeleteWorkspaceV2) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{5}
}

func (x *DeleteWorkspaceV2) GetOrgId() string {
	if x != nil {
		return x.OrgId
	}
	return ""
}

func (x *DeleteWorkspaceV2) GetWorkspaceId() uint64 {
	if x != nil {
		return x.WorkspaceId
	}
	return 0
}

type SetWorkspaceV2_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Workspace *WorkspaceV2 `protobuf:"bytes,1,opt,name=workspace,proto3" json:"workspace,omitempty"`
}

func (x *SetWorkspaceV2_Response) Reset() {
	*x = SetWorkspaceV2_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[6]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *SetWorkspaceV2_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*SetWorkspaceV2_Response) ProtoMessage() {}

func (x *SetWorkspaceV2_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[6]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use SetWorkspaceV2_Response.ProtoReflect.Descriptor instead.
func (*SetWorkspaceV2_Response) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{2, 0}
}

func (x *SetWorkspaceV2_Response) GetWorkspace() *WorkspaceV2 {
	if x != nil {
		return x.Workspace
	}
	return nil
}

type GetWorkspaceV2_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Workspace *WorkspaceV2 `protobuf:"bytes,1,opt,name=workspace,proto3" json:"workspace,omitempty"`
}

func (x *GetWorkspaceV2_Response) Reset() {
	*x = GetWorkspaceV2_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[7]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetWorkspaceV2_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetWorkspaceV2_Response) ProtoMessage() {}

func (x *GetWorkspaceV2_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[7]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetWorkspaceV2_Response.ProtoReflect.Descriptor instead.
func (*GetWorkspaceV2_Response) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{3, 0}
}

func (x *GetWorkspaceV2_Response) GetWorkspace() *WorkspaceV2 {
	if x != nil {
		return x.Workspace
	}
	return nil
}

type SearchWorkspacesV2_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Workspaces   []*WorkspaceV2     `protobuf:"bytes,1,rep,name=workspaces,proto3" json:"workspaces,omitempty"`
	TotalRecords int64              `protobuf:"varint,2,opt,name=total_records,json=totalRecords,proto3" json:"total_records,omitempty"`
	Pagination   *common.Pagination `protobuf:"bytes,3,opt,name=pagination,proto3" json:"pagination,omitempty"`
}

func (x *SearchWorkspacesV2_Response) Reset() {
	*x = SearchWorkspacesV2_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[8]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *SearchWorkspacesV2_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*SearchWorkspacesV2_Response) ProtoMessage() {}

func (x *SearchWorkspacesV2_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[8]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use SearchWorkspacesV2_Response.ProtoReflect.Descriptor instead.
func (*SearchWorkspacesV2_Response) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{4, 0}
}

func (x *SearchWorkspacesV2_Response) GetWorkspaces() []*WorkspaceV2 {
	if x != nil {
		return x.Workspaces
	}
	return nil
}

func (x *SearchWorkspacesV2_Response) GetTotalRecords() int64 {
	if x != nil {
		return x.TotalRecords
	}
	return 0
}

func (x *SearchWorkspacesV2_Response) GetPagination() *common.Pagination {
	if x != nil {
		return x.Pagination
	}
	return nil
}

type DeleteWorkspaceV2_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields
}

func (x *DeleteWorkspaceV2_Response) Reset() {
	*x = DeleteWorkspaceV2_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_WorkspaceV2_proto_msgTypes[9]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *DeleteWorkspaceV2_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*DeleteWorkspaceV2_Response) ProtoMessage() {}

func (x *DeleteWorkspaceV2_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_WorkspaceV2_proto_msgTypes[9]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use DeleteWorkspaceV2_Response.ProtoReflect.Descriptor instead.
func (*DeleteWorkspaceV2_Response) Descriptor() ([]byte, []int) {
	return file_uac_WorkspaceV2_proto_rawDescGZIP(), []int{5, 0}
}

var File_uac_WorkspaceV2_proto protoreflect.FileDescriptor

var file_uac_WorkspaceV2_proto_rawDesc = []byte{
	0x0a, 0x15, 0x75, 0x61, 0x63, 0x2f, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56,
	0x32, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x0c, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74,
	0x61, 0x2e, 0x75, 0x61, 0x63, 0x1a, 0x1c, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, 0x2f, 0x61, 0x70,
	0x69, 0x2f, 0x61, 0x6e, 0x6e, 0x6f, 0x74, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x2e, 0x70, 0x72,
	0x6f, 0x74, 0x6f, 0x1a, 0x1a, 0x63, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x2f, 0x43, 0x6f, 0x6d, 0x6d,
	0x6f, 0x6e, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22,
	0x40, 0x0a, 0x0a, 0x50, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x12, 0x19, 0x0a,
	0x08, 0x67, 0x72, 0x6f, 0x75, 0x70, 0x5f, 0x69, 0x64, 0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52,
	0x07, 0x67, 0x72, 0x6f, 0x75, 0x70, 0x49, 0x64, 0x12, 0x17, 0x0a, 0x07, 0x72, 0x6f, 0x6c, 0x65,
	0x5f, 0x69, 0x64, 0x18, 0x03, 0x20, 0x01, 0x28, 0x09, 0x52, 0x06, 0x72, 0x6f, 0x6c, 0x65, 0x49,
	0x64, 0x22, 0xb9, 0x02, 0x0a, 0x0b, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56,
	0x32, 0x12, 0x15, 0x0a, 0x06, 0x6f, 0x72, 0x67, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28,
	0x09, 0x52, 0x05, 0x6f, 0x72, 0x67, 0x49, 0x64, 0x12, 0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18, 0x02,
	0x20, 0x01, 0x28, 0x04, 0x52, 0x02, 0x69, 0x64, 0x12, 0x12, 0x0a, 0x04, 0x6e, 0x61, 0x6d, 0x65,
	0x18, 0x03, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x12, 0x20, 0x0a, 0x0b,
	0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x18, 0x04, 0x20, 0x01, 0x28,
	0x09, 0x52, 0x0b, 0x64, 0x65, 0x73, 0x63, 0x72, 0x69, 0x70, 0x74, 0x69, 0x6f, 0x6e, 0x12, 0x3a,
	0x0a, 0x0b, 0x70, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x18, 0x05, 0x20,
	0x03, 0x28, 0x0b, 0x32, 0x18, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75,
	0x61, 0x63, 0x2e, 0x50, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x52, 0x0b, 0x70,
	0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x12, 0x2b, 0x0a, 0x11, 0x63, 0x72,
	0x65, 0x61, 0x74, 0x65, 0x64, 0x5f, 0x74, 0x69, 0x6d, 0x65, 0x73, 0x74, 0x61, 0x6d, 0x70, 0x18,
	0x07, 0x20, 0x01, 0x28, 0x03, 0x52, 0x10, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x54, 0x69,
	0x6d, 0x65, 0x73, 0x74, 0x61, 0x6d, 0x70, 0x12, 0x2b, 0x0a, 0x11, 0x75, 0x70, 0x64, 0x61, 0x74,
	0x65, 0x64, 0x5f, 0x74, 0x69, 0x6d, 0x65, 0x73, 0x74, 0x61, 0x6d, 0x70, 0x18, 0x08, 0x20, 0x01,
	0x28, 0x03, 0x52, 0x10, 0x75, 0x70, 0x64, 0x61, 0x74, 0x65, 0x64, 0x54, 0x69, 0x6d, 0x65, 0x73,
	0x74, 0x61, 0x6d, 0x70, 0x12, 0x1c, 0x0a, 0x09, 0x6e, 0x61, 0x6d, 0x65, 0x73, 0x70, 0x61, 0x63,
	0x65, 0x18, 0x09, 0x20, 0x01, 0x28, 0x09, 0x52, 0x09, 0x6e, 0x61, 0x6d, 0x65, 0x73, 0x70, 0x61,
	0x63, 0x65, 0x12, 0x19, 0x0a, 0x08, 0x62, 0x75, 0x69, 0x6c, 0x74, 0x5f, 0x69, 0x6e, 0x18, 0x0a,
	0x20, 0x01, 0x28, 0x08, 0x52, 0x07, 0x62, 0x75, 0x69, 0x6c, 0x74, 0x49, 0x6e, 0x22, 0x8e, 0x01,
	0x0a, 0x0e, 0x53, 0x65, 0x74, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32,
	0x12, 0x37, 0x0a, 0x09, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x18, 0x02, 0x20,
	0x01, 0x28, 0x0b, 0x32, 0x19, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75,
	0x61, 0x63, 0x2e, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x52, 0x09,
	0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x1a, 0x43, 0x0a, 0x08, 0x52, 0x65, 0x73,
	0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x37, 0x0a, 0x09, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61,
	0x63, 0x65, 0x18, 0x01, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x19, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65,
	0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63,
	0x65, 0x56, 0x32, 0x52, 0x09, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x22, 0xb5,
	0x01, 0x0a, 0x0e, 0x47, 0x65, 0x74, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56,
	0x32, 0x12, 0x15, 0x0a, 0x06, 0x6f, 0x72, 0x67, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28,
	0x09, 0x52, 0x05, 0x6f, 0x72, 0x67, 0x49, 0x64, 0x12, 0x23, 0x0a, 0x0c, 0x77, 0x6f, 0x72, 0x6b,
	0x73, 0x70, 0x61, 0x63, 0x65, 0x5f, 0x69, 0x64, 0x18, 0x02, 0x20, 0x01, 0x28, 0x04, 0x48, 0x00,
	0x52, 0x0b, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x49, 0x64, 0x12, 0x14, 0x0a,
	0x04, 0x6e, 0x61, 0x6d, 0x65, 0x18, 0x03, 0x20, 0x01, 0x28, 0x09, 0x48, 0x00, 0x52, 0x04, 0x6e,
	0x61, 0x6d, 0x65, 0x1a, 0x43, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12,
	0x37, 0x0a, 0x09, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x18, 0x01, 0x20, 0x01,
	0x28, 0x0b, 0x32, 0x19, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61,
	0x63, 0x2e, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x52, 0x09, 0x77,
	0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x42, 0x0c, 0x0a, 0x0a, 0x69, 0x64, 0x65, 0x6e,
	0x74, 0x69, 0x66, 0x69, 0x65, 0x72, 0x22, 0x92, 0x02, 0x0a, 0x12, 0x53, 0x65, 0x61, 0x72, 0x63,
	0x68, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x73, 0x56, 0x32, 0x12, 0x15, 0x0a,
	0x06, 0x6f, 0x72, 0x67, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x05, 0x6f,
	0x72, 0x67, 0x49, 0x64, 0x12, 0x3b, 0x0a, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69,
	0x6f, 0x6e, 0x18, 0x02, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x1b, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65,
	0x72, 0x74, 0x61, 0x2e, 0x63, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x2e, 0x50, 0x61, 0x67, 0x69, 0x6e,
	0x61, 0x74, 0x69, 0x6f, 0x6e, 0x52, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f,
	0x6e, 0x1a, 0xa7, 0x01, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x39,
	0x0a, 0x0a, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x73, 0x18, 0x01, 0x20, 0x03,
	0x28, 0x0b, 0x32, 0x19, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61,
	0x63, 0x2e, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x52, 0x0a, 0x77,
	0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x73, 0x12, 0x23, 0x0a, 0x0d, 0x74, 0x6f, 0x74,
	0x61, 0x6c, 0x5f, 0x72, 0x65, 0x63, 0x6f, 0x72, 0x64, 0x73, 0x18, 0x02, 0x20, 0x01, 0x28, 0x03,
	0x52, 0x0c, 0x74, 0x6f, 0x74, 0x61, 0x6c, 0x52, 0x65, 0x63, 0x6f, 0x72, 0x64, 0x73, 0x12, 0x3b,
	0x0a, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x18, 0x03, 0x20, 0x01,
	0x28, 0x0b, 0x32, 0x1b, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x63, 0x6f,
	0x6d, 0x6d, 0x6f, 0x6e, 0x2e, 0x50, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x52,
	0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x22, 0x59, 0x0a, 0x11, 0x44,
	0x65, 0x6c, 0x65, 0x74, 0x65, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32,
	0x12, 0x15, 0x0a, 0x06, 0x6f, 0x72, 0x67, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09,
	0x52, 0x05, 0x6f, 0x72, 0x67, 0x49, 0x64, 0x12, 0x21, 0x0a, 0x0c, 0x77, 0x6f, 0x72, 0x6b, 0x73,
	0x70, 0x61, 0x63, 0x65, 0x5f, 0x69, 0x64, 0x18, 0x02, 0x20, 0x01, 0x28, 0x04, 0x52, 0x0b, 0x77,
	0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x49, 0x64, 0x1a, 0x0a, 0x0a, 0x08, 0x52, 0x65,
	0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x32, 0xe3, 0x04, 0x0a, 0x12, 0x57, 0x6f, 0x72, 0x6b, 0x73,
	0x70, 0x61, 0x63, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x56, 0x32, 0x12, 0x8e, 0x01,
	0x0a, 0x0c, 0x73, 0x65, 0x74, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x12, 0x1c,
	0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x53, 0x65,
	0x74, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x1a, 0x25, 0x2e, 0x61,
	0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x53, 0x65, 0x74, 0x57,
	0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f,
	0x6e, 0x73, 0x65, 0x22, 0x39, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x33, 0x3a, 0x01, 0x2a, 0x22, 0x2e,
	0x2f, 0x76, 0x32, 0x2f, 0x6f, 0x72, 0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e,
	0x2f, 0x7b, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x2e, 0x6f, 0x72, 0x67, 0x5f,
	0x69, 0x64, 0x7d, 0x2f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x73, 0x12, 0x90,
	0x01, 0x0a, 0x0c, 0x67, 0x65, 0x74, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x12,
	0x1c, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47,
	0x65, 0x74, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x1a, 0x25, 0x2e,
	0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74,
	0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x2e, 0x52, 0x65, 0x73, 0x70,
	0x6f, 0x6e, 0x73, 0x65, 0x22, 0x3b, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x35, 0x12, 0x33, 0x2f, 0x76,
	0x32, 0x2f, 0x6f, 0x72, 0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x2f, 0x7b,
	0x6f, 0x72, 0x67, 0x5f, 0x69, 0x64, 0x7d, 0x2f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63,
	0x65, 0x73, 0x2f, 0x7b, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x5f, 0x69, 0x64,
	0x7d, 0x12, 0x98, 0x01, 0x0a, 0x0f, 0x64, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x57, 0x6f, 0x72, 0x6b,
	0x73, 0x70, 0x61, 0x63, 0x65, 0x12, 0x1f, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61,
	0x2e, 0x75, 0x61, 0x63, 0x2e, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x57, 0x6f, 0x72, 0x6b, 0x73,
	0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x1a, 0x28, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74,
	0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x44, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x57, 0x6f, 0x72, 0x6b,
	0x73, 0x70, 0x61, 0x63, 0x65, 0x56, 0x32, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65,
	0x22, 0x3a, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x34, 0x2a, 0x32, 0x2f, 0x76, 0x32, 0x2f, 0x6f, 0x72,
	0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x2f, 0x7b, 0x6f, 0x72, 0x67, 0x5f,
	0x69, 0x64, 0x7d, 0x2f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x2f, 0x7b, 0x77,
	0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x5f, 0x69, 0x64, 0x7d, 0x12, 0x8d, 0x01, 0x0a,
	0x10, 0x73, 0x65, 0x61, 0x72, 0x63, 0x68, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65,
	0x73, 0x12, 0x20, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63,
	0x2e, 0x53, 0x65, 0x61, 0x72, 0x63, 0x68, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65,
	0x73, 0x56, 0x32, 0x1a, 0x29, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75,
	0x61, 0x63, 0x2e, 0x53, 0x65, 0x61, 0x72, 0x63, 0x68, 0x57, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61,
	0x63, 0x65, 0x73, 0x56, 0x32, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x2c,
	0x82, 0xd3, 0xe4, 0x93, 0x02, 0x26, 0x12, 0x24, 0x2f, 0x76, 0x32, 0x2f, 0x6f, 0x72, 0x67, 0x61,
	0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x2f, 0x7b, 0x6f, 0x72, 0x67, 0x5f, 0x69, 0x64,
	0x7d, 0x2f, 0x77, 0x6f, 0x72, 0x6b, 0x73, 0x70, 0x61, 0x63, 0x65, 0x73, 0x42, 0x3e, 0x50, 0x01,
	0x5a, 0x3a, 0x67, 0x69, 0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x56, 0x65, 0x72,
	0x74, 0x61, 0x41, 0x49, 0x2f, 0x6d, 0x6f, 0x64, 0x65, 0x6c, 0x64, 0x62, 0x2f, 0x70, 0x72, 0x6f,
	0x74, 0x6f, 0x73, 0x2f, 0x67, 0x65, 0x6e, 0x2f, 0x67, 0x6f, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f,
	0x73, 0x2f, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, 0x2f, 0x75, 0x61, 0x63, 0x62, 0x06, 0x70, 0x72,
	0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_uac_WorkspaceV2_proto_rawDescOnce sync.Once
	file_uac_WorkspaceV2_proto_rawDescData = file_uac_WorkspaceV2_proto_rawDesc
)

func file_uac_WorkspaceV2_proto_rawDescGZIP() []byte {
	file_uac_WorkspaceV2_proto_rawDescOnce.Do(func() {
		file_uac_WorkspaceV2_proto_rawDescData = protoimpl.X.CompressGZIP(file_uac_WorkspaceV2_proto_rawDescData)
	})
	return file_uac_WorkspaceV2_proto_rawDescData
}

var file_uac_WorkspaceV2_proto_msgTypes = make([]protoimpl.MessageInfo, 10)
var file_uac_WorkspaceV2_proto_goTypes = []interface{}{
	(*Permission)(nil),                  // 0: ai.verta.uac.Permission
	(*WorkspaceV2)(nil),                 // 1: ai.verta.uac.WorkspaceV2
	(*SetWorkspaceV2)(nil),              // 2: ai.verta.uac.SetWorkspaceV2
	(*GetWorkspaceV2)(nil),              // 3: ai.verta.uac.GetWorkspaceV2
	(*SearchWorkspacesV2)(nil),          // 4: ai.verta.uac.SearchWorkspacesV2
	(*DeleteWorkspaceV2)(nil),           // 5: ai.verta.uac.DeleteWorkspaceV2
	(*SetWorkspaceV2_Response)(nil),     // 6: ai.verta.uac.SetWorkspaceV2.Response
	(*GetWorkspaceV2_Response)(nil),     // 7: ai.verta.uac.GetWorkspaceV2.Response
	(*SearchWorkspacesV2_Response)(nil), // 8: ai.verta.uac.SearchWorkspacesV2.Response
	(*DeleteWorkspaceV2_Response)(nil),  // 9: ai.verta.uac.DeleteWorkspaceV2.Response
	(*common.Pagination)(nil),           // 10: ai.verta.common.Pagination
}
var file_uac_WorkspaceV2_proto_depIdxs = []int32{
	0,  // 0: ai.verta.uac.WorkspaceV2.permissions:type_name -> ai.verta.uac.Permission
	1,  // 1: ai.verta.uac.SetWorkspaceV2.workspace:type_name -> ai.verta.uac.WorkspaceV2
	10, // 2: ai.verta.uac.SearchWorkspacesV2.pagination:type_name -> ai.verta.common.Pagination
	1,  // 3: ai.verta.uac.SetWorkspaceV2.Response.workspace:type_name -> ai.verta.uac.WorkspaceV2
	1,  // 4: ai.verta.uac.GetWorkspaceV2.Response.workspace:type_name -> ai.verta.uac.WorkspaceV2
	1,  // 5: ai.verta.uac.SearchWorkspacesV2.Response.workspaces:type_name -> ai.verta.uac.WorkspaceV2
	10, // 6: ai.verta.uac.SearchWorkspacesV2.Response.pagination:type_name -> ai.verta.common.Pagination
	2,  // 7: ai.verta.uac.WorkspaceServiceV2.setWorkspace:input_type -> ai.verta.uac.SetWorkspaceV2
	3,  // 8: ai.verta.uac.WorkspaceServiceV2.getWorkspace:input_type -> ai.verta.uac.GetWorkspaceV2
	5,  // 9: ai.verta.uac.WorkspaceServiceV2.deleteWorkspace:input_type -> ai.verta.uac.DeleteWorkspaceV2
	4,  // 10: ai.verta.uac.WorkspaceServiceV2.searchWorkspaces:input_type -> ai.verta.uac.SearchWorkspacesV2
	6,  // 11: ai.verta.uac.WorkspaceServiceV2.setWorkspace:output_type -> ai.verta.uac.SetWorkspaceV2.Response
	7,  // 12: ai.verta.uac.WorkspaceServiceV2.getWorkspace:output_type -> ai.verta.uac.GetWorkspaceV2.Response
	9,  // 13: ai.verta.uac.WorkspaceServiceV2.deleteWorkspace:output_type -> ai.verta.uac.DeleteWorkspaceV2.Response
	8,  // 14: ai.verta.uac.WorkspaceServiceV2.searchWorkspaces:output_type -> ai.verta.uac.SearchWorkspacesV2.Response
	11, // [11:15] is the sub-list for method output_type
	7,  // [7:11] is the sub-list for method input_type
	7,  // [7:7] is the sub-list for extension type_name
	7,  // [7:7] is the sub-list for extension extendee
	0,  // [0:7] is the sub-list for field type_name
}

func init() { file_uac_WorkspaceV2_proto_init() }
func file_uac_WorkspaceV2_proto_init() {
	if File_uac_WorkspaceV2_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_uac_WorkspaceV2_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Permission); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*WorkspaceV2); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*SetWorkspaceV2); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetWorkspaceV2); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[4].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*SearchWorkspacesV2); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[5].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*DeleteWorkspaceV2); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[6].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*SetWorkspaceV2_Response); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[7].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetWorkspaceV2_Response); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[8].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*SearchWorkspacesV2_Response); i {
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
		file_uac_WorkspaceV2_proto_msgTypes[9].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*DeleteWorkspaceV2_Response); i {
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
	file_uac_WorkspaceV2_proto_msgTypes[3].OneofWrappers = []interface{}{
		(*GetWorkspaceV2_WorkspaceId)(nil),
		(*GetWorkspaceV2_Name)(nil),
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_uac_WorkspaceV2_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   10,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_uac_WorkspaceV2_proto_goTypes,
		DependencyIndexes: file_uac_WorkspaceV2_proto_depIdxs,
		MessageInfos:      file_uac_WorkspaceV2_proto_msgTypes,
	}.Build()
	File_uac_WorkspaceV2_proto = out.File
	file_uac_WorkspaceV2_proto_rawDesc = nil
	file_uac_WorkspaceV2_proto_goTypes = nil
	file_uac_WorkspaceV2_proto_depIdxs = nil
}
