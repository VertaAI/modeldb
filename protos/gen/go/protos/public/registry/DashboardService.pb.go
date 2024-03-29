// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.31.0
// 	protoc        v3.11.2
// source: registry/DashboardService.proto

package registry

import (
	context "context"
	_ "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	_ "github.com/VertaAI/modeldb/protos/gen/go/protos/public/registry"
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

// A summary value displayed as single number on a card
type Summary struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Name  string  `protobuf:"bytes,1,opt,name=name,proto3" json:"name,omitempty"`
	Value float64 `protobuf:"fixed64,2,opt,name=value,proto3" json:"value,omitempty"`
}

func (x *Summary) Reset() {
	*x = Summary{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Summary) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Summary) ProtoMessage() {}

func (x *Summary) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Summary.ProtoReflect.Descriptor instead.
func (*Summary) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{0}
}

func (x *Summary) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

func (x *Summary) GetValue() float64 {
	if x != nil {
		return x.Value
	}
	return 0
}

// One value in a chart
type ChartValue struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Name  string  `protobuf:"bytes,1,opt,name=name,proto3" json:"name,omitempty"`
	Value float64 `protobuf:"fixed64,2,opt,name=value,proto3" json:"value,omitempty"`
}

func (x *ChartValue) Reset() {
	*x = ChartValue{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *ChartValue) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*ChartValue) ProtoMessage() {}

func (x *ChartValue) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use ChartValue.ProtoReflect.Descriptor instead.
func (*ChartValue) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{1}
}

func (x *ChartValue) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

func (x *ChartValue) GetValue() float64 {
	if x != nil {
		return x.Value
	}
	return 0
}

// A chart containing a list of items
// For exmple a pie chart with incidents per business unit would have a list business unit names and the sum of incidents for that unit.
type Chart struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Name string `protobuf:"bytes,1,opt,name=name,proto3" json:"name,omitempty"`
	// Deprecated: Marked as deprecated in registry/DashboardService.proto.
	Items       map[string]float32 `protobuf:"bytes,2,rep,name=items,proto3" json:"items,omitempty" protobuf_key:"bytes,1,opt,name=key,proto3" protobuf_val:"fixed32,2,opt,name=value,proto3"` // Name-value items to be charted
	ChartValues []*ChartValue      `protobuf:"bytes,3,rep,name=chart_values,json=chartValues,proto3" json:"chart_values,omitempty"`
}

func (x *Chart) Reset() {
	*x = Chart{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Chart) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Chart) ProtoMessage() {}

func (x *Chart) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Chart.ProtoReflect.Descriptor instead.
func (*Chart) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{2}
}

func (x *Chart) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

// Deprecated: Marked as deprecated in registry/DashboardService.proto.
func (x *Chart) GetItems() map[string]float32 {
	if x != nil {
		return x.Items
	}
	return nil
}

func (x *Chart) GetChartValues() []*ChartValue {
	if x != nil {
		return x.ChartValues
	}
	return nil
}

// A dashboard containing one or more elements
type Dashboard struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Name      string     `protobuf:"bytes,1,opt,name=name,proto3" json:"name,omitempty"`
	Summaries []*Summary `protobuf:"bytes,2,rep,name=summaries,proto3" json:"summaries,omitempty"` // Summary card
	Charts    []*Chart   `protobuf:"bytes,3,rep,name=charts,proto3" json:"charts,omitempty"`
}

func (x *Dashboard) Reset() {
	*x = Dashboard{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Dashboard) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Dashboard) ProtoMessage() {}

func (x *Dashboard) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Dashboard.ProtoReflect.Descriptor instead.
func (*Dashboard) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{3}
}

func (x *Dashboard) GetName() string {
	if x != nil {
		return x.Name
	}
	return ""
}

func (x *Dashboard) GetSummaries() []*Summary {
	if x != nil {
		return x.Summaries
	}
	return nil
}

func (x *Dashboard) GetCharts() []*Chart {
	if x != nil {
		return x.Charts
	}
	return nil
}

type GetDashboard struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	OrganizationId string `protobuf:"bytes,1,opt,name=organization_id,json=organizationId,proto3" json:"organization_id,omitempty"` // The organization id to use when calculating dashboard values and items.
}

func (x *GetDashboard) Reset() {
	*x = GetDashboard{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[4]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetDashboard) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetDashboard) ProtoMessage() {}

func (x *GetDashboard) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[4]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetDashboard.ProtoReflect.Descriptor instead.
func (*GetDashboard) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{4}
}

func (x *GetDashboard) GetOrganizationId() string {
	if x != nil {
		return x.OrganizationId
	}
	return ""
}

type DashboardModel struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Id uint64 `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`
}

func (x *DashboardModel) Reset() {
	*x = DashboardModel{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[5]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *DashboardModel) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*DashboardModel) ProtoMessage() {}

func (x *DashboardModel) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[5]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use DashboardModel.ProtoReflect.Descriptor instead.
func (*DashboardModel) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{5}
}

func (x *DashboardModel) GetId() uint64 {
	if x != nil {
		return x.Id
	}
	return 0
}

type TopModels struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	OrganizationId string `protobuf:"bytes,1,opt,name=organization_id,json=organizationId,proto3" json:"organization_id,omitempty"` // The organization id to use for filtering models.
	Limit          int32  `protobuf:"varint,2,opt,name=limit,proto3" json:"limit,omitempty"`                                        // The maximum number of models to return
	SortBy         string `protobuf:"bytes,3,opt,name=sort_by,json=sortBy,proto3" json:"sort_by,omitempty"`                         // The name of the attribute to sort by
}

func (x *TopModels) Reset() {
	*x = TopModels{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[6]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *TopModels) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*TopModels) ProtoMessage() {}

func (x *TopModels) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[6]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use TopModels.ProtoReflect.Descriptor instead.
func (*TopModels) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{6}
}

func (x *TopModels) GetOrganizationId() string {
	if x != nil {
		return x.OrganizationId
	}
	return ""
}

func (x *TopModels) GetLimit() int32 {
	if x != nil {
		return x.Limit
	}
	return 0
}

func (x *TopModels) GetSortBy() string {
	if x != nil {
		return x.SortBy
	}
	return ""
}

type GetDashboard_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Dashboard *Dashboard `protobuf:"bytes,1,opt,name=dashboard,proto3" json:"dashboard,omitempty"`
}

func (x *GetDashboard_Response) Reset() {
	*x = GetDashboard_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[8]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *GetDashboard_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*GetDashboard_Response) ProtoMessage() {}

func (x *GetDashboard_Response) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[8]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use GetDashboard_Response.ProtoReflect.Descriptor instead.
func (*GetDashboard_Response) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{4, 0}
}

func (x *GetDashboard_Response) GetDashboard() *Dashboard {
	if x != nil {
		return x.Dashboard
	}
	return nil
}

type TopModels_Response struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Models []*DashboardModel `protobuf:"bytes,1,rep,name=models,proto3" json:"models,omitempty"`
}

func (x *TopModels_Response) Reset() {
	*x = TopModels_Response{}
	if protoimpl.UnsafeEnabled {
		mi := &file_registry_DashboardService_proto_msgTypes[9]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *TopModels_Response) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*TopModels_Response) ProtoMessage() {}

func (x *TopModels_Response) ProtoReflect() protoreflect.Message {
	mi := &file_registry_DashboardService_proto_msgTypes[9]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use TopModels_Response.ProtoReflect.Descriptor instead.
func (*TopModels_Response) Descriptor() ([]byte, []int) {
	return file_registry_DashboardService_proto_rawDescGZIP(), []int{6, 0}
}

func (x *TopModels_Response) GetModels() []*DashboardModel {
	if x != nil {
		return x.Models
	}
	return nil
}

var File_registry_DashboardService_proto protoreflect.FileDescriptor

var file_registry_DashboardService_proto_rawDesc = []byte{
	0x0a, 0x1f, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2f, 0x44, 0x61, 0x73, 0x68, 0x62,
	0x6f, 0x61, 0x72, 0x64, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x12, 0x11, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69,
	0x73, 0x74, 0x72, 0x79, 0x1a, 0x1a, 0x63, 0x6f, 0x6d, 0x6d, 0x6f, 0x6e, 0x2f, 0x43, 0x6f, 0x6d,
	0x6d, 0x6f, 0x6e, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f,
	0x1a, 0x1c, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, 0x2f, 0x61, 0x70, 0x69, 0x2f, 0x61, 0x6e, 0x6e,
	0x6f, 0x74, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x1a, 0x25,
	0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2f, 0x43, 0x75, 0x73, 0x74, 0x6f, 0x6d, 0x41,
	0x74, 0x74, 0x72, 0x69, 0x62, 0x75, 0x74, 0x65, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x2e,
	0x70, 0x72, 0x6f, 0x74, 0x6f, 0x22, 0x33, 0x0a, 0x07, 0x53, 0x75, 0x6d, 0x6d, 0x61, 0x72, 0x79,
	0x12, 0x12, 0x0a, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04,
	0x6e, 0x61, 0x6d, 0x65, 0x12, 0x14, 0x0a, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x18, 0x02, 0x20,
	0x01, 0x28, 0x01, 0x52, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x22, 0x36, 0x0a, 0x0a, 0x43, 0x68,
	0x61, 0x72, 0x74, 0x56, 0x61, 0x6c, 0x75, 0x65, 0x12, 0x12, 0x0a, 0x04, 0x6e, 0x61, 0x6d, 0x65,
	0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x12, 0x14, 0x0a, 0x05,
	0x76, 0x61, 0x6c, 0x75, 0x65, 0x18, 0x02, 0x20, 0x01, 0x28, 0x01, 0x52, 0x05, 0x76, 0x61, 0x6c,
	0x75, 0x65, 0x22, 0xd6, 0x01, 0x0a, 0x05, 0x43, 0x68, 0x61, 0x72, 0x74, 0x12, 0x12, 0x0a, 0x04,
	0x6e, 0x61, 0x6d, 0x65, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04, 0x6e, 0x61, 0x6d, 0x65,
	0x12, 0x3d, 0x0a, 0x05, 0x69, 0x74, 0x65, 0x6d, 0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x0b, 0x32,
	0x23, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73,
	0x74, 0x72, 0x79, 0x2e, 0x43, 0x68, 0x61, 0x72, 0x74, 0x2e, 0x49, 0x74, 0x65, 0x6d, 0x73, 0x45,
	0x6e, 0x74, 0x72, 0x79, 0x42, 0x02, 0x18, 0x01, 0x52, 0x05, 0x69, 0x74, 0x65, 0x6d, 0x73, 0x12,
	0x40, 0x0a, 0x0c, 0x63, 0x68, 0x61, 0x72, 0x74, 0x5f, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x73, 0x18,
	0x03, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x1d, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61,
	0x2e, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2e, 0x43, 0x68, 0x61, 0x72, 0x74, 0x56,
	0x61, 0x6c, 0x75, 0x65, 0x52, 0x0b, 0x63, 0x68, 0x61, 0x72, 0x74, 0x56, 0x61, 0x6c, 0x75, 0x65,
	0x73, 0x1a, 0x38, 0x0a, 0x0a, 0x49, 0x74, 0x65, 0x6d, 0x73, 0x45, 0x6e, 0x74, 0x72, 0x79, 0x12,
	0x10, 0x0a, 0x03, 0x6b, 0x65, 0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x03, 0x6b, 0x65,
	0x79, 0x12, 0x14, 0x0a, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x18, 0x02, 0x20, 0x01, 0x28, 0x02,
	0x52, 0x05, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x3a, 0x02, 0x38, 0x01, 0x22, 0x8b, 0x01, 0x0a, 0x09,
	0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x12, 0x12, 0x0a, 0x04, 0x6e, 0x61, 0x6d,
	0x65, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x04, 0x6e, 0x61, 0x6d, 0x65, 0x12, 0x38, 0x0a,
	0x09, 0x73, 0x75, 0x6d, 0x6d, 0x61, 0x72, 0x69, 0x65, 0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x0b,
	0x32, 0x1a, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69,
	0x73, 0x74, 0x72, 0x79, 0x2e, 0x53, 0x75, 0x6d, 0x6d, 0x61, 0x72, 0x79, 0x52, 0x09, 0x73, 0x75,
	0x6d, 0x6d, 0x61, 0x72, 0x69, 0x65, 0x73, 0x12, 0x30, 0x0a, 0x06, 0x63, 0x68, 0x61, 0x72, 0x74,
	0x73, 0x18, 0x03, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x18, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2e, 0x43, 0x68, 0x61, 0x72,
	0x74, 0x52, 0x06, 0x63, 0x68, 0x61, 0x72, 0x74, 0x73, 0x22, 0x7f, 0x0a, 0x0c, 0x47, 0x65, 0x74,
	0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x12, 0x27, 0x0a, 0x0f, 0x6f, 0x72, 0x67,
	0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01,
	0x28, 0x09, 0x52, 0x0e, 0x6f, 0x72, 0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e,
	0x49, 0x64, 0x1a, 0x46, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x3a,
	0x0a, 0x09, 0x64, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28,
	0x0b, 0x32, 0x1c, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67,
	0x69, 0x73, 0x74, 0x72, 0x79, 0x2e, 0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x52,
	0x09, 0x64, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x22, 0x20, 0x0a, 0x0e, 0x44, 0x61,
	0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x4d, 0x6f, 0x64, 0x65, 0x6c, 0x12, 0x0e, 0x0a, 0x02,
	0x69, 0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x04, 0x52, 0x02, 0x69, 0x64, 0x22, 0xaa, 0x01, 0x0a,
	0x09, 0x54, 0x6f, 0x70, 0x4d, 0x6f, 0x64, 0x65, 0x6c, 0x73, 0x12, 0x27, 0x0a, 0x0f, 0x6f, 0x72,
	0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20,
	0x01, 0x28, 0x09, 0x52, 0x0e, 0x6f, 0x72, 0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f,
	0x6e, 0x49, 0x64, 0x12, 0x14, 0x0a, 0x05, 0x6c, 0x69, 0x6d, 0x69, 0x74, 0x18, 0x02, 0x20, 0x01,
	0x28, 0x05, 0x52, 0x05, 0x6c, 0x69, 0x6d, 0x69, 0x74, 0x12, 0x17, 0x0a, 0x07, 0x73, 0x6f, 0x72,
	0x74, 0x5f, 0x62, 0x79, 0x18, 0x03, 0x20, 0x01, 0x28, 0x09, 0x52, 0x06, 0x73, 0x6f, 0x72, 0x74,
	0x42, 0x79, 0x1a, 0x45, 0x0a, 0x08, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x12, 0x39,
	0x0a, 0x06, 0x6d, 0x6f, 0x64, 0x65, 0x6c, 0x73, 0x18, 0x01, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x21,
	0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74,
	0x72, 0x79, 0x2e, 0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x4d, 0x6f, 0x64, 0x65,
	0x6c, 0x52, 0x06, 0x6d, 0x6f, 0x64, 0x65, 0x6c, 0x73, 0x32, 0x89, 0x02, 0x0a, 0x10, 0x44, 0x61,
	0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63, 0x65, 0x12, 0x79,
	0x0a, 0x0c, 0x67, 0x65, 0x74, 0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x12, 0x1f,
	0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74,
	0x72, 0x79, 0x2e, 0x47, 0x65, 0x74, 0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x1a,
	0x28, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73,
	0x74, 0x72, 0x79, 0x2e, 0x47, 0x65, 0x74, 0x44, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64,
	0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x1e, 0x82, 0xd3, 0xe4, 0x93, 0x02,
	0x18, 0x12, 0x16, 0x2f, 0x76, 0x31, 0x2f, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2f,
	0x64, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x12, 0x7a, 0x0a, 0x0c, 0x67, 0x65, 0x74,
	0x54, 0x6f, 0x70, 0x4d, 0x6f, 0x64, 0x65, 0x6c, 0x73, 0x12, 0x1c, 0x2e, 0x61, 0x69, 0x2e, 0x76,
	0x65, 0x72, 0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2e, 0x54, 0x6f,
	0x70, 0x4d, 0x6f, 0x64, 0x65, 0x6c, 0x73, 0x1a, 0x25, 0x2e, 0x61, 0x69, 0x2e, 0x76, 0x65, 0x72,
	0x74, 0x61, 0x2e, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x2e, 0x54, 0x6f, 0x70, 0x4d,
	0x6f, 0x64, 0x65, 0x6c, 0x73, 0x2e, 0x52, 0x65, 0x73, 0x70, 0x6f, 0x6e, 0x73, 0x65, 0x22, 0x25,
	0x82, 0xd3, 0xe4, 0x93, 0x02, 0x1f, 0x12, 0x1d, 0x2f, 0x76, 0x31, 0x2f, 0x72, 0x65, 0x67, 0x69,
	0x73, 0x74, 0x72, 0x79, 0x2f, 0x64, 0x61, 0x73, 0x68, 0x62, 0x6f, 0x61, 0x72, 0x64, 0x2f, 0x6d,
	0x6f, 0x64, 0x65, 0x6c, 0x73, 0x42, 0x47, 0x50, 0x01, 0x5a, 0x43, 0x67, 0x69, 0x74, 0x68, 0x75,
	0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x56, 0x65, 0x72, 0x74, 0x61, 0x41, 0x49, 0x2f, 0x70, 0x72,
	0x6f, 0x74, 0x6f, 0x73, 0x2d, 0x61, 0x6c, 0x6c, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f,
	0x67, 0x65, 0x6e, 0x2f, 0x67, 0x6f, 0x2f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x73, 0x2f, 0x70, 0x72,
	0x69, 0x76, 0x61, 0x74, 0x65, 0x2f, 0x72, 0x65, 0x67, 0x69, 0x73, 0x74, 0x72, 0x79, 0x62, 0x06,
	0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_registry_DashboardService_proto_rawDescOnce sync.Once
	file_registry_DashboardService_proto_rawDescData = file_registry_DashboardService_proto_rawDesc
)

func file_registry_DashboardService_proto_rawDescGZIP() []byte {
	file_registry_DashboardService_proto_rawDescOnce.Do(func() {
		file_registry_DashboardService_proto_rawDescData = protoimpl.X.CompressGZIP(file_registry_DashboardService_proto_rawDescData)
	})
	return file_registry_DashboardService_proto_rawDescData
}

var file_registry_DashboardService_proto_msgTypes = make([]protoimpl.MessageInfo, 10)
var file_registry_DashboardService_proto_goTypes = []interface{}{
	(*Summary)(nil),               // 0: ai.verta.registry.Summary
	(*ChartValue)(nil),            // 1: ai.verta.registry.ChartValue
	(*Chart)(nil),                 // 2: ai.verta.registry.Chart
	(*Dashboard)(nil),             // 3: ai.verta.registry.Dashboard
	(*GetDashboard)(nil),          // 4: ai.verta.registry.GetDashboard
	(*DashboardModel)(nil),        // 5: ai.verta.registry.DashboardModel
	(*TopModels)(nil),             // 6: ai.verta.registry.TopModels
	nil,                           // 7: ai.verta.registry.Chart.ItemsEntry
	(*GetDashboard_Response)(nil), // 8: ai.verta.registry.GetDashboard.Response
	(*TopModels_Response)(nil),    // 9: ai.verta.registry.TopModels.Response
}
var file_registry_DashboardService_proto_depIdxs = []int32{
	7, // 0: ai.verta.registry.Chart.items:type_name -> ai.verta.registry.Chart.ItemsEntry
	1, // 1: ai.verta.registry.Chart.chart_values:type_name -> ai.verta.registry.ChartValue
	0, // 2: ai.verta.registry.Dashboard.summaries:type_name -> ai.verta.registry.Summary
	2, // 3: ai.verta.registry.Dashboard.charts:type_name -> ai.verta.registry.Chart
	3, // 4: ai.verta.registry.GetDashboard.Response.dashboard:type_name -> ai.verta.registry.Dashboard
	5, // 5: ai.verta.registry.TopModels.Response.models:type_name -> ai.verta.registry.DashboardModel
	4, // 6: ai.verta.registry.DashboardService.getDashboard:input_type -> ai.verta.registry.GetDashboard
	6, // 7: ai.verta.registry.DashboardService.getTopModels:input_type -> ai.verta.registry.TopModels
	8, // 8: ai.verta.registry.DashboardService.getDashboard:output_type -> ai.verta.registry.GetDashboard.Response
	9, // 9: ai.verta.registry.DashboardService.getTopModels:output_type -> ai.verta.registry.TopModels.Response
	8, // [8:10] is the sub-list for method output_type
	6, // [6:8] is the sub-list for method input_type
	6, // [6:6] is the sub-list for extension type_name
	6, // [6:6] is the sub-list for extension extendee
	0, // [0:6] is the sub-list for field type_name
}

func init() { file_registry_DashboardService_proto_init() }
func file_registry_DashboardService_proto_init() {
	if File_registry_DashboardService_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_registry_DashboardService_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Summary); i {
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
		file_registry_DashboardService_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*ChartValue); i {
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
		file_registry_DashboardService_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Chart); i {
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
		file_registry_DashboardService_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Dashboard); i {
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
		file_registry_DashboardService_proto_msgTypes[4].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetDashboard); i {
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
		file_registry_DashboardService_proto_msgTypes[5].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*DashboardModel); i {
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
		file_registry_DashboardService_proto_msgTypes[6].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*TopModels); i {
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
		file_registry_DashboardService_proto_msgTypes[8].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*GetDashboard_Response); i {
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
		file_registry_DashboardService_proto_msgTypes[9].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*TopModels_Response); i {
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
			RawDescriptor: file_registry_DashboardService_proto_rawDesc,
			NumEnums:      0,
			NumMessages:   10,
			NumExtensions: 0,
			NumServices:   1,
		},
		GoTypes:           file_registry_DashboardService_proto_goTypes,
		DependencyIndexes: file_registry_DashboardService_proto_depIdxs,
		MessageInfos:      file_registry_DashboardService_proto_msgTypes,
	}.Build()
	File_registry_DashboardService_proto = out.File
	file_registry_DashboardService_proto_rawDesc = nil
	file_registry_DashboardService_proto_goTypes = nil
	file_registry_DashboardService_proto_depIdxs = nil
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConnInterface

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// DashboardServiceClient is the client API for DashboardService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type DashboardServiceClient interface {
	// Gets information for a named dashboard. organization_id can be provided as a query parameter.
	GetDashboard(ctx context.Context, in *GetDashboard, opts ...grpc.CallOption) (*GetDashboard_Response, error)
	// Gets a list of models sorted by model attribute
	GetTopModels(ctx context.Context, in *TopModels, opts ...grpc.CallOption) (*TopModels_Response, error)
}

type dashboardServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewDashboardServiceClient(cc grpc.ClientConnInterface) DashboardServiceClient {
	return &dashboardServiceClient{cc}
}

func (c *dashboardServiceClient) GetDashboard(ctx context.Context, in *GetDashboard, opts ...grpc.CallOption) (*GetDashboard_Response, error) {
	out := new(GetDashboard_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.DashboardService/getDashboard", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *dashboardServiceClient) GetTopModels(ctx context.Context, in *TopModels, opts ...grpc.CallOption) (*TopModels_Response, error) {
	out := new(TopModels_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.DashboardService/getTopModels", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// DashboardServiceServer is the server API for DashboardService service.
type DashboardServiceServer interface {
	// Gets information for a named dashboard. organization_id can be provided as a query parameter.
	GetDashboard(context.Context, *GetDashboard) (*GetDashboard_Response, error)
	// Gets a list of models sorted by model attribute
	GetTopModels(context.Context, *TopModels) (*TopModels_Response, error)
}

// UnimplementedDashboardServiceServer can be embedded to have forward compatible implementations.
type UnimplementedDashboardServiceServer struct {
}

func (*UnimplementedDashboardServiceServer) GetDashboard(context.Context, *GetDashboard) (*GetDashboard_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetDashboard not implemented")
}
func (*UnimplementedDashboardServiceServer) GetTopModels(context.Context, *TopModels) (*TopModels_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetTopModels not implemented")
}

func RegisterDashboardServiceServer(s *grpc.Server, srv DashboardServiceServer) {
	s.RegisterService(&_DashboardService_serviceDesc, srv)
}

func _DashboardService_GetDashboard_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(GetDashboard)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DashboardServiceServer).GetDashboard(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.DashboardService/GetDashboard",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DashboardServiceServer).GetDashboard(ctx, req.(*GetDashboard))
	}
	return interceptor(ctx, in, info, handler)
}

func _DashboardService_GetTopModels_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(TopModels)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(DashboardServiceServer).GetTopModels(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.DashboardService/GetTopModels",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(DashboardServiceServer).GetTopModels(ctx, req.(*TopModels))
	}
	return interceptor(ctx, in, info, handler)
}

var _DashboardService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ai.verta.registry.DashboardService",
	HandlerType: (*DashboardServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "getDashboard",
			Handler:    _DashboardService_GetDashboard_Handler,
		},
		{
			MethodName: "getTopModels",
			Handler:    _DashboardService_GetTopModels_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "registry/DashboardService.proto",
}
