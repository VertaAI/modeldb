// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.31.0
// 	protoc        v3.11.2
// source: uac/Authorization.proto

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

// Answers the question "what `entities` are allowed to perform any of `actions` on any of `resources`?"
// Lists all entities that are allowed to perform a certain action on certain resources
type GetAllowedEntities struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Actions    []*Action          `protobuf:"bytes,1,rep,name=actions,proto3" json:"actions,omitempty"`
	Resources  []*Resources       `protobuf:"bytes,2,rep,name=resources,proto3" json:"resources,omitempty"`
	Pagination *common.Pagination `protobuf:"bytes,3,opt,name=pagination,proto3" json:"pagination,omitempty"`
}

func (x *GetAllowedEntities) Reset() {
	*x = GetAllowedEntities{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetAllowedEntities) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetAllowedEntities) ProtoMessage() {}

func (x *GetAllowedEntities) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetAllowedEntities.ProtoReflect.Descriptor instead.
func (*GetAllowedEntities) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{0}
}

func (x *GetAllowedEntities) GetActions() []*Action {
	if x != nil {
		return x.Actions
	}
	return nil
}

func (x *GetAllowedEntities) GetResources() []*Resources {
	if x != nil {
		return x.Resources
	}
	return nil
}

func (x *GetAllowedEntities) GetPagination() *common.Pagination {
	if x != nil {
		return x.Pagination
	}
	return nil
}

type GetAllowedEntitiesWithActionsResponseItem struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Entities []*Entities `protobuf:"bytes,1,rep,name=entities,proto3" json:"entities,omitempty"`
	Actions  *Action     `protobuf:"bytes,2,opt,name=actions,proto3" json:"actions,omitempty"`
}

func (x *GetAllowedEntitiesWithActionsResponseItem) Reset() {
	*x = GetAllowedEntitiesWithActionsResponseItem{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetAllowedEntitiesWithActionsResponseItem) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetAllowedEntitiesWithActionsResponseItem) ProtoMessage() {}

func (x *GetAllowedEntitiesWithActionsResponseItem) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetAllowedEntitiesWithActionsResponseItem.ProtoReflect.Descriptor instead.
func (*GetAllowedEntitiesWithActionsResponseItem) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{1}
}

func (x *GetAllowedEntitiesWithActionsResponseItem) GetEntities() []*Entities {
	if x != nil {
		return x.Entities
	}
	return nil
}

func (x *GetAllowedEntitiesWithActionsResponseItem) GetActions() *Action {
	if x != nil {
		return x.Actions
	}
	return nil
}

// Same as IsAllowed, but infers entities from the current logged user
type IsSelfAllowed struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Actions   []*Action    `protobuf:"bytes,2,rep,name=actions,proto3" json:"actions,omitempty"`
	Resources []*Resources `protobuf:"bytes,3,rep,name=resources,proto3" json:"resources,omitempty"`
}

func (x *IsSelfAllowed) Reset() {
	*x = IsSelfAllowed{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *IsSelfAllowed) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*IsSelfAllowed) ProtoMessage() {}

func (x *IsSelfAllowed) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use IsSelfAllowed.ProtoReflect.Descriptor instead.
func (*IsSelfAllowed) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{2}
}

func (x *IsSelfAllowed) GetActions() []*Action {
	if x != nil {
		return x.Actions
	}
	return nil
}

func (x *IsSelfAllowed) GetResources() []*Resources {
	if x != nil {
		return x.Resources
	}
	return nil
}

// Same as GetAllowedResources, but infers entities from the current logged user
type GetSelfAllowedResources struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Actions      []*Action           `protobuf:"bytes,2,rep,name=actions,proto3" json:"actions,omitempty"`
	ResourceType *ResourceType       `protobuf:"bytes,3,opt,name=resource_type,json=resourceType,proto3" json:"resource_type,omitempty"`
	Service      ServiceEnum_Service `protobuf:"varint,4,opt,name=service,proto3,enum=ai.verta.uac.ServiceEnum_Service" json:"service,omitempty"`
	Pagination   *common.Pagination  `protobuf:"bytes,5,opt,name=pagination,proto3" json:"pagination,omitempty"`
}

func (x *GetSelfAllowedResources) Reset() {
	*x = GetSelfAllowedResources{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetSelfAllowedResources) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetSelfAllowedResources) ProtoMessage() {}

func (x *GetSelfAllowedResources) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetSelfAllowedResources.ProtoReflect.Descriptor instead.
func (*GetSelfAllowedResources) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{3}
}

func (x *GetSelfAllowedResources) GetActions() []*Action {
	if x != nil {
		return x.Actions
	}
	return nil
}

func (x *GetSelfAllowedResources) GetResourceType() *ResourceType {
	if x != nil {
		return x.ResourceType
	}
	return nil
}

func (x *GetSelfAllowedResources) GetService() ServiceEnum_Service {
	if x != nil {
		return x.Service
	}
	return ServiceEnum_UNKNOWN
}

func (x *GetSelfAllowedResources) GetPagination() *common.Pagination {
	if x != nil {
		return x.Pagination
	}
	return nil
}

type Actions struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Actions []*Action `protobuf:"bytes,2,rep,name=actions,proto3" json:"actions,omitempty"`
}

func (x *Actions) Reset() {
	*x = Actions{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[4]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Actions) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Actions) ProtoMessage() {}

func (x *Actions) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[4]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Actions.ProtoReflect.Descriptor instead.
func (*Actions) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{4}
}

func (x *Actions) GetActions() []*Action {
	if x != nil {
		return x.Actions
	}
	return nil
}

type GetSelfAllowedActionsBatch struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Resources *Resources `protobuf:"bytes,3,opt,name=resources,proto3" json:"resources,omitempty"`
}

func (x *GetSelfAllowedActionsBatch) Reset() {
	*x = GetSelfAllowedActionsBatch{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[5]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetSelfAllowedActionsBatch) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetSelfAllowedActionsBatch) ProtoMessage() {}

func (x *GetSelfAllowedActionsBatch) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[5]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetSelfAllowedActionsBatch.ProtoReflect.Descriptor instead.
func (*GetSelfAllowedActionsBatch) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{5}
}

func (x *GetSelfAllowedActionsBatch) GetResources() *Resources {
	if x != nil {
		return x.Resources
	}
	return nil
}

type GetAllowedEntities_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Entities     []*Entities `protobuf:"bytes,1,rep,name=entities,proto3" json:"entities,omitempty"`
	TotalRecords int64       `protobuf:"varint,2,opt,name=total_records,json=totalRecords,proto3" json:"total_records,omitempty"`
}

func (x *GetAllowedEntities_Response) Reset() {
	*x = GetAllowedEntities_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[6]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetAllowedEntities_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetAllowedEntities_Response) ProtoMessage() {}

func (x *GetAllowedEntities_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[6]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetAllowedEntities_Response.ProtoReflect.Descriptor instead.
func (*GetAllowedEntities_Response) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{0, 0}
}

func (x *GetAllowedEntities_Response) GetEntities() []*Entities {
	if x != nil {
		return x.Entities
	}
	return nil
}

func (x *GetAllowedEntities_Response) GetTotalRecords() int64 {
	if x != nil {
		return x.TotalRecords
	}
	return 0
}

type IsSelfAllowed_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Allowed bool `protobuf:"varint,1,opt,name=allowed,proto3" json:"allowed,omitempty"`
}

func (x *IsSelfAllowed_Response) Reset() {
	*x = IsSelfAllowed_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[7]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *IsSelfAllowed_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*IsSelfAllowed_Response) ProtoMessage() {}

func (x *IsSelfAllowed_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[7]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use IsSelfAllowed_Response.ProtoReflect.Descriptor instead.
func (*IsSelfAllowed_Response) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{2, 0}
}

func (x *IsSelfAllowed_Response) GetAllowed() bool {
	if x != nil {
		return x.Allowed
	}
	return false
}

type GetSelfAllowedResources_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Resources    []*Resources `protobuf:"bytes,1,rep,name=resources,proto3" json:"resources,omitempty"`
	TotalRecords int64        `protobuf:"varint,2,opt,name=total_records,json=totalRecords,proto3" json:"total_records,omitempty"`
}

func (x *GetSelfAllowedResources_Response) Reset() {
	*x = GetSelfAllowedResources_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[8]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetSelfAllowedResources_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetSelfAllowedResources_Response) ProtoMessage() {}

func (x *GetSelfAllowedResources_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[8]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetSelfAllowedResources_Response.ProtoReflect.Descriptor instead.
func (*GetSelfAllowedResources_Response) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{3, 0}
}

func (x *GetSelfAllowedResources_Response) GetResources() []*Resources {
	if x != nil {
		return x.Resources
	}
	return nil
}

func (x *GetSelfAllowedResources_Response) GetTotalRecords() int64 {
	if x != nil {
		return x.TotalRecords
	}
	return 0
}

type GetSelfAllowedActionsBatch_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Actions map[string]*Actions `protobuf:"bytes,2,rep,name=actions,proto3" json:"actions,omitempty" protobuf_key:"bytes,1,opt,name=key,proto3" protobuf_val:"bytes,2,opt,name=value,proto3"` // key is resource id
}

func (x *GetSelfAllowedActionsBatch_Response) Reset() {
	*x = GetSelfAllowedActionsBatch_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_uac_Authorization_proto_msgTypes[9]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetSelfAllowedActionsBatch_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetSelfAllowedActionsBatch_Response) ProtoMessage() {}

func (x *GetSelfAllowedActionsBatch_Response) ProtoReflect() protoreflect.Message {
	mi := &file_uac_Authorization_proto_msgTypes[9]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetSelfAllowedActionsBatch_Response.ProtoReflect.Descriptor instead.
func (*GetSelfAllowedActionsBatch_Response) Descriptor() ([]byte, []int) {
	return file_uac_Authorization_proto_rawDescGZIP(), []int{5, 0}
}

func (x *GetSelfAllowedActionsBatch_Response) GetActions() map[string]*Actions {
	if x != nil {
		return x.Actions
	}
	return nil
}

var File_uac_Authorization_proto protoreflect.FileDescriptor

var file_uac_Authorization_proto_rawDesc = []byte{
	0x0a, 0x17, 0x75, 0x61, 0x63, 0x2f, 0x41, 0x75, 0x74, 0x68, 0x6f, 0x72, 0x69, 0x7a, 0x61, 0x74,
	0x69, 0x6f, 0x6e, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x0c, 0x61, 0x69, 0x2e, 0x76, 0x65,
	0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x1a, 0x1c, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, 0x2f,
	0x61, 0x70, 0x69, 0x2f, 0x61, 0x6e, 0x6e, 0x6f, 0x74, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x2e,
	0x70, 0x72, 0x6f, 0x74, 0x6f, 0x1a, 0x15, 0x75, 0x61, 0x63, 0x2f, 0x52, 0x6f, 0x6c, 0x65, 0x53,
	0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x1a, 0x1a, 0x63, 0x6f,
	0x6d, 0x6d, 0x6f, 0x6e, 0x2f, 0x43, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x53, 0x65, 0x72, 0x76, 0x69,
	0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0x9d, 0x02, 0x0a, 0x12, 0x47, 0x65, 0x74,
	0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x12,
	0x2e, 0x0a, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x0b,
	0x32, 0x14, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e,
	0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x52, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x12,
	0x35, 0x0a, 0x09, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x18, 0x02, 0x20, 0x03,
	0x28, 0x0b, 0x32, 0x17, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61,
	0x63, 0x2e, 0x52, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x52, 0x09, 0x72, 0x65, 0x73,
	0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x12, 0x3b, 0x0a, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61,
	0x74, 0x69, 0x6f, 0x6e, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x1b, 0x2e, 0x61, 0x69, 0x2e,
	0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x63, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x2e, 0x50, 0x61, 0x67,
	0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x52, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74,
	0x69, 0x6f, 0x6e, 0x1a, 0x63, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12,
	0x32, 0x0a, 0x08, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28,
	0x0b, 0x32, 0x16, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63,
	0x2e, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x52, 0x08, 0x65, 0x6e, 0x74, 0x69, 0x74,
	0x69, 0x65, 0x73, 0x12, 0x23, 0x0a, 0x0d, 0x74, 0x6f, 0x74, 0x61, 0x6c, 0x5f, 0x72, 0x65, 0x63,
	0x6f, 0x72, 0x64, 0x73, 0x18, 0x02, 0x20, 0x01, 0x28, 0x03, 0x52, 0x0c, 0x74, 0x6f, 0x74, 0x61,
	0x6c, 0x52, 0x65, 0x63, 0x6f, 0x72, 0x64, 0x73, 0x22, 0x8f, 0x01, 0x0a, 0x29, 0x47, 0x65, 0x74,
	0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x57,
	0x69, 0x74, 0x68, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e,
	0x73, 0x65, 0x49, 0x74, 0x65, 0x6d, 0x12, 0x32, 0x0a, 0x08, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x69,
	0x65, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x16, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65,
	0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73,
	0x52, 0x08, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x12, 0x2e, 0x0a, 0x07, 0x61, 0x63,
	0x74, 0x69, 0x6f, 0x6e, 0x73, 0x18, 0x02, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x14, 0x2e, 0x61, 0x69,
	0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x41, 0x63, 0x74, 0x69, 0x6f,
	0x6e, 0x52, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x22, 0x9c, 0x01, 0x0a, 0x0d, 0x49,
	0x73, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x12, 0x2e, 0x0a, 0x07,
	0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x14, 0x2e,
	0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x41, 0x63, 0x74,
	0x69, 0x6f, 0x6e, 0x52, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x12, 0x35, 0x0a, 0x09,
	0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x18, 0x03, 0x20, 0x03, 0x28, 0x0b, 0x32,
	0x17, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x52,
	0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x52, 0x09, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72,
	0x63, 0x65, 0x73, 0x1a, 0x24, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12,
	0x18, 0x0a, 0x07, 0x61, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x08,
	0x52, 0x07, 0x61, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x22, 0xec, 0x02, 0x0a, 0x17, 0x47, 0x65,
	0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x52, 0x65, 0x73, 0x6f,
	0x75, 0x72, 0x63, 0x65, 0x73, 0x12, 0x2e, 0x0a, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73,
	0x18, 0x02, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x14, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74,
	0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x52, 0x07, 0x61, 0x63,
	0x74, 0x69, 0x6f, 0x6e, 0x73, 0x12, 0x3f, 0x0a, 0x0d, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63,
	0x65, 0x5f, 0x74, 0x79, 0x70, 0x65, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x1a, 0x2e, 0x61,
	0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x52, 0x65, 0x73, 0x6f,
	0x75, 0x72, 0x63, 0x65, 0x54, 0x79, 0x70, 0x65, 0x52, 0x0c, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72,
	0x63, 0x65, 0x54, 0x79, 0x70, 0x65, 0x12, 0x3b, 0x0a, 0x07, 0x73, 0x65, 0x72, 0x76, 0x69, 0x63,
	0x65, 0x18, 0x04, 0x20, 0x01, 0x28, 0x0e, 0x32, 0x21, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x45, 0x6e,
	0x75, 0x6d, 0x2e, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x52, 0x07, 0x73, 0x65, 0x72, 0x76,
	0x69, 0x63, 0x65, 0x12, 0x3b, 0x0a, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f,
	0x6e, 0x18, 0x05, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x1b, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x63, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x2e, 0x50, 0x61, 0x67, 0x69, 0x6e, 0x61,
	0x74, 0x69, 0x6f, 0x6e, 0x52, 0x0a, 0x70, 0x61, 0x67, 0x69, 0x6e, 0x61, 0x74, 0x69, 0x6f, 0x6e,
	0x1a, 0x66, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x35, 0x0a, 0x09,
	0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x0b, 0x32,
	0x17, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x52,
	0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x52, 0x09, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72,
	0x63, 0x65, 0x73, 0x12, 0x23, 0x0a, 0x0d, 0x74, 0x6f, 0x74, 0x61, 0x6c, 0x5f, 0x72, 0x65, 0x63,
	0x6f, 0x72, 0x64, 0x73, 0x18, 0x02, 0x20, 0x01, 0x28, 0x03, 0x52, 0x0c, 0x74, 0x6f, 0x74, 0x61,
	0x6c, 0x52, 0x65, 0x63, 0x6f, 0x72, 0x64, 0x73, 0x22, 0x39, 0x0a, 0x07, 0x41, 0x63, 0x74, 0x69,
	0x6f, 0x6e, 0x73, 0x12, 0x2e, 0x0a, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x18, 0x02,
	0x20, 0x03, 0x28, 0x0b, 0x32, 0x14, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e,
	0x75, 0x61, 0x63, 0x2e, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x52, 0x07, 0x61, 0x63, 0x74, 0x69,
	0x6f, 0x6e, 0x73, 0x22, 0x8d, 0x02, 0x0a, 0x1a, 0x47, 0x65, 0x74, 0x53, 0x65, 0x6c, 0x66, 0x41,
	0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x42, 0x61, 0x74,
	0x63, 0x68, 0x12, 0x35, 0x0a, 0x09, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x18,
	0x03, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x17, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61,
	0x2e, 0x75, 0x61, 0x63, 0x2e, 0x52, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x52, 0x09,
	0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x1a, 0xb7, 0x01, 0x0a, 0x08, 0x52, 0x65,
	0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x58, 0x0a, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e,
	0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x3e, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c,
	0x6c, 0x6f, 0x77, 0x65, 0x64, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x42, 0x61, 0x74, 0x63,
	0x68, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x2e, 0x41, 0x63, 0x74, 0x69, 0x6f,
	0x6e, 0x73, 0x45, 0x6e, 0x74, 0x72, 0x79, 0x52, 0x07, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73,
	0x1a, 0x51, 0x0a, 0x0c, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x45, 0x6e, 0x74, 0x72, 0x79,
	0x12, 0x10, 0x0a, 0x03, 0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x03, 0x6b,
	0x65, 0x79, 0x12, 0x2b, 0x0a, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x18, 0x02, 0x20, 0x01, 0x28,
	0x0b, 0x32, 0x15, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63,
	0x2e, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x52, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x3a,
	0x02, 0x38, 0x01, 0x32, 0xe1, 0x04, 0x0a, 0x0c, 0x41, 0x75, 0x74, 0x68, 0x7a, 0x53, 0x65, 0x72,
	0x76, 0x69, 0x63, 0x65, 0x12, 0x8a, 0x01, 0x0a, 0x12, 0x67, 0x65, 0x74, 0x41, 0x6c, 0x6c, 0x6f,
	0x77, 0x65, 0x64, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x12, 0x20, 0x2e, 0x61, 0x69,
	0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74, 0x41, 0x6c,
	0x6c, 0x6f, 0x77, 0x65, 0x64, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x1a, 0x29, 0x2e,
	0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74,
	0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65, 0x73, 0x2e,
	0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x27, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x21,
	0x3a, 0x01, 0x2a, 0x22, 0x1c, 0x2f, 0x76, 0x31, 0x2f, 0x61, 0x75, 0x74, 0x68, 0x7a, 0x2f, 0x67,
	0x65, 0x74, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x69, 0x65,
	0x73, 0x12, 0x76, 0x0a, 0x0d, 0x69, 0x73, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77,
	0x65, 0x64, 0x12, 0x1b, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61,
	0x63, 0x2e, 0x49, 0x73, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x1a,
	0x24, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x49,
	0x73, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x2e, 0x52, 0x65, 0x73,
	0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x22, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x1c, 0x3a, 0x01, 0x2a,
	0x22, 0x17, 0x2f, 0x76, 0x31, 0x2f, 0x61, 0x75, 0x74, 0x68, 0x7a, 0x2f, 0x69, 0x73, 0x53, 0x65,
	0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x12, 0x9e, 0x01, 0x0a, 0x17, 0x67, 0x65,
	0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x52, 0x65, 0x73, 0x6f,
	0x75, 0x72, 0x63, 0x65, 0x73, 0x12, 0x25, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61,
	0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f,
	0x77, 0x65, 0x64, 0x52, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x1a, 0x2e, 0x2e, 0x61,
	0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74, 0x53,
	0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x52, 0x65, 0x73, 0x6f, 0x75, 0x72,
	0x63, 0x65, 0x73, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x2c, 0x82, 0xd3,
	0xe4, 0x93, 0x02, 0x26, 0x3a, 0x01, 0x2a, 0x22, 0x21, 0x2f, 0x76, 0x31, 0x2f, 0x61, 0x75, 0x74,
	0x68, 0x7a, 0x2f, 0x67, 0x65, 0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65,
	0x64, 0x52, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x12, 0xaa, 0x01, 0x0a, 0x1a, 0x67,
	0x65, 0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x41, 0x63, 0x74,
	0x69, 0x6f, 0x6e, 0x73, 0x42, 0x61, 0x74, 0x63, 0x68, 0x12, 0x28, 0x2e, 0x61, 0x69, 0x2e, 0x76,
	0x65, 0x72, 0x74, 0x61, 0x2e, 0x75, 0x61, 0x63, 0x2e, 0x47, 0x65, 0x74, 0x53, 0x65, 0x6c, 0x66,
	0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x42, 0x61,
	0x74, 0x63, 0x68, 0x1a, 0x31, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x75,
	0x61, 0x63, 0x2e, 0x47, 0x65, 0x74, 0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65,
	0x64, 0x41, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x42, 0x61, 0x74, 0x63, 0x68, 0x2e, 0x52, 0x65,
	0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x2f, 0x82, 0xd3, 0xe4, 0x93, 0x02, 0x29, 0x3a, 0x01,
	0x2a, 0x22, 0x24, 0x2f, 0x76, 0x31, 0x2f, 0x61, 0x75, 0x74, 0x68, 0x7a, 0x2f, 0x67, 0x65, 0x74,
	0x53, 0x65, 0x6c, 0x66, 0x41, 0x6c, 0x6c, 0x6f, 0x77, 0x65, 0x64, 0x41, 0x63, 0x74, 0x69, 0x6f,
	0x6e, 0x73, 0x42, 0x61, 0x74, 0x63, 0x68, 0x42, 0x3e, 0x50, 0x01, 0x5a, 0x3a, 0x67, 0x69, 0x74,
	0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x56, 0x65, 0x72, 0x74, 0x61, 0x41, 0x49, 0x2f,
	0x6d, 0x6f, 0x64, 0x65, 0x6c, 0x64, 0x62, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f, 0x67,
	0x65, 0x6e, 0x2f, 0x67, 0x6f, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f, 0x70, 0x75, 0x62,
	0x6c, 0x69, 0x63, 0x2f, 0x75, 0x61, 0x63, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_uac_Authorization_proto_rawDescOnce sync.Once
	file_uac_Authorization_proto_rawDescData = file_uac_Authorization_proto_rawDesc
)

func file_uac_Authorization_proto_rawDescGZIP() []byte {
	file_uac_Authorization_proto_rawDescOnce.Do(func() {
		file_uac_Authorization_proto_rawDescData = protoimpl.X.CompressGZIP(file_uac_Authorization_proto_rawDescData)
	})
	return file_uac_Authorization_proto_rawDescData
}

var file_uac_Authorization_proto_msgTypes = make([]protoimpl.MessageInfo, 11)
var file_uac_Authorization_proto_goTypes = []interface{}{
	(*GetAllowedEntities)(nil),                        // 0: ai.verta.uac.GetAllowedEntities
	(*GetAllowedEntitiesWithActionsResponseItem)(nil), // 1: ai.verta.uac.GetAllowedEntitiesWithActionsResponseItem
	(*IsSelfAllowed)(nil),                             // 2: ai.verta.uac.IsSelfAllowed
	(*GetSelfAllowedResources)(nil),                   // 3: ai.verta.uac.GetSelfAllowedResources
	(*Actions)(nil),                                   // 4: ai.verta.uac.Actions
	(*GetSelfAllowedActionsBatch)(nil),                // 5: ai.verta.uac.GetSelfAllowedActionsBatch
	(*GetAllowedEntities_Response)(nil),               // 6: ai.verta.uac.GetAllowedEntities.Response
	(*IsSelfAllowed_Response)(nil),                    // 7: ai.verta.uac.IsSelfAllowed.Response
	(*GetSelfAllowedResources_Response)(nil),          // 8: ai.verta.uac.GetSelfAllowedResources.Response
	(*GetSelfAllowedActionsBatch_Response)(nil),       // 9: ai.verta.uac.GetSelfAllowedActionsBatch.Response
	nil,                       // 10: ai.verta.uac.GetSelfAllowedActionsBatch.Response.ActionsEntry
	(*Action)(nil),            // 11: ai.verta.uac.Action
	(*Resources)(nil),         // 12: ai.verta.uac.Resources
	(*common.Pagination)(nil), // 13: ai.verta.common.Pagination
	(*Entities)(nil),          // 14: ai.verta.uac.Entities
	(*ResourceType)(nil),      // 15: ai.verta.uac.ResourceType
	(ServiceEnum_Service)(0),  // 16: ai.verta.uac.ServiceEnum.Service
}
var file_uac_Authorization_proto_depIdxs = []int32{
	11, // 0: ai.verta.uac.GetAllowedEntities.actions:type_name -> ai.verta.uac.Action
	12, // 1: ai.verta.uac.GetAllowedEntities.resources:type_name -> ai.verta.uac.Resources
	13, // 2: ai.verta.uac.GetAllowedEntities.pagination:type_name -> ai.verta.common.Pagination
	14, // 3: ai.verta.uac.GetAllowedEntitiesWithActionsResponseItem.entities:type_name -> ai.verta.uac.Entities
	11, // 4: ai.verta.uac.GetAllowedEntitiesWithActionsResponseItem.actions:type_name -> ai.verta.uac.Action
	11, // 5: ai.verta.uac.IsSelfAllowed.actions:type_name -> ai.verta.uac.Action
	12, // 6: ai.verta.uac.IsSelfAllowed.resources:type_name -> ai.verta.uac.Resources
	11, // 7: ai.verta.uac.GetSelfAllowedResources.actions:type_name -> ai.verta.uac.Action
	15, // 8: ai.verta.uac.GetSelfAllowedResources.resource_type:type_name -> ai.verta.uac.ResourceType
	16, // 9: ai.verta.uac.GetSelfAllowedResources.service:type_name -> ai.verta.uac.ServiceEnum.Service
	13, // 10: ai.verta.uac.GetSelfAllowedResources.pagination:type_name -> ai.verta.common.Pagination
	11, // 11: ai.verta.uac.Actions.actions:type_name -> ai.verta.uac.Action
	12, // 12: ai.verta.uac.GetSelfAllowedActionsBatch.resources:type_name -> ai.verta.uac.Resources
	14, // 13: ai.verta.uac.GetAllowedEntities.Response.entities:type_name -> ai.verta.uac.Entities
	12, // 14: ai.verta.uac.GetSelfAllowedResources.Response.resources:type_name -> ai.verta.uac.Resources
	10, // 15: ai.verta.uac.GetSelfAllowedActionsBatch.Response.actions:type_name -> ai.verta.uac.GetSelfAllowedActionsBatch.Response.ActionsEntry
	4,  // 16: ai.verta.uac.GetSelfAllowedActionsBatch.Response.ActionsEntry.value:type_name -> ai.verta.uac.Actions
	0,  // 17: ai.verta.uac.AuthzService.getAllowedEntities:input_type -> ai.verta.uac.GetAllowedEntities
	2,  // 18: ai.verta.uac.AuthzService.isSelfAllowed:input_type -> ai.verta.uac.IsSelfAllowed
	3,  // 19: ai.verta.uac.AuthzService.getSelfAllowedResources:input_type -> ai.verta.uac.GetSelfAllowedResources
	5,  // 20: ai.verta.uac.AuthzService.getSelfAllowedActionsBatch:input_type -> ai.verta.uac.GetSelfAllowedActionsBatch
	6,  // 21: ai.verta.uac.AuthzService.getAllowedEntities:output_type -> ai.verta.uac.GetAllowedEntities.Response
	7,  // 22: ai.verta.uac.AuthzService.isSelfAllowed:output_type -> ai.verta.uac.IsSelfAllowed.Response
	8,  // 23: ai.verta.uac.AuthzService.getSelfAllowedResources:output_type -> ai.verta.uac.GetSelfAllowedResources.Response
	9,  // 24: ai.verta.uac.AuthzService.getSelfAllowedActionsBatch:output_type -> ai.verta.uac.GetSelfAllowedActionsBatch.Response
	21, // [21:25] is the sub-list for method output_type
	17, // [17:21] is the sub-list for method input_type
	17, // [17:17] is the sub-list for extension type_name
	17, // [17:17] is the sub-list for extension extendee
	0,  // [0:17] is the sub-list for field type_name
}

func init() { file_uac_Authorization_proto_init() }
func file_uac_Authorization_proto_init() {
	if File_uac_Authorization_proto != nil {
		return
	}
	file_uac_RoleService_proto_init()
	if !protoimpl.UnsafeEnabled {
		file_uac_Authorization_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetAllowedEntities); i {
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
		file_uac_Authorization_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetAllowedEntitiesWithActionsResponseItem); i {
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
		file_uac_Authorization_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*IsSelfAllowed); i {
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
		file_uac_Authorization_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetSelfAllowedResources); i {
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
		file_uac_Authorization_proto_msgTypes[4].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Actions); i {
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
		file_uac_Authorization_proto_msgTypes[5].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetSelfAllowedActionsBatch); i {
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
		file_uac_Authorization_proto_msgTypes[6].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetAllowedEntities_Response); i {
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
		file_uac_Authorization_proto_msgTypes[7].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*IsSelfAllowed_Response); i {
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
		file_uac_Authorization_proto_msgTypes[8].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetSelfAllowedResources_Response); i {
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
		file_uac_Authorization_proto_msgTypes[9].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetSelfAllowedActionsBatch_Response); i {
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
			RawDescriptor: file_uac_Authorization_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   11,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_uac_Authorization_proto_goTypes,
		DependencyIndexes: file_uac_Authorization_proto_depIdxs,
		MessageInfos:      file_uac_Authorization_proto_msgTypes,
	}.Build()
	File_uac_Authorization_proto = out.File
	file_uac_Authorization_proto_rawDesc = nil
	file_uac_Authorization_proto_goTypes = nil
	file_uac_Authorization_proto_depIdxs = nil
}
