// Code generated by protoc-gen-go-grpc. DO NOT EDIT.

package monitoring

import (
	context "context"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
)

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// MonitoredEntityServiceClient is the client API for MonitoredEntityService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type MonitoredEntityServiceClient interface {
	CreateMonitoredEntity(ctx context.Context, in *CreateMonitoredEntityRequest, opts ...grpc.CallOption) (*CreateMonitoredEntityRequest_Response, error)
	UpdateMonitoredEntity(ctx context.Context, in *UpdateMonitoredEntityRequest, opts ...grpc.CallOption) (*UpdateMonitoredEntityRequest_Response, error)
	FindMonitoredEntity(ctx context.Context, in *FindMonitoredEntityRequest, opts ...grpc.CallOption) (*FindMonitoredEntityRequest_Response, error)
	DeleteMonitoredEntity(ctx context.Context, in *DeleteMonitoredEntityRequest, opts ...grpc.CallOption) (*DeleteMonitoredEntityRequest_Response, error)
}

type monitoredEntityServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewMonitoredEntityServiceClient(cc grpc.ClientConnInterface) MonitoredEntityServiceClient {
	return &monitoredEntityServiceClient{cc}
}

func (c *monitoredEntityServiceClient) CreateMonitoredEntity(ctx context.Context, in *CreateMonitoredEntityRequest, opts ...grpc.CallOption) (*CreateMonitoredEntityRequest_Response, error) {
	out := new(CreateMonitoredEntityRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.monitoring.MonitoredEntityService/createMonitoredEntity", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *monitoredEntityServiceClient) UpdateMonitoredEntity(ctx context.Context, in *UpdateMonitoredEntityRequest, opts ...grpc.CallOption) (*UpdateMonitoredEntityRequest_Response, error) {
	out := new(UpdateMonitoredEntityRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.monitoring.MonitoredEntityService/updateMonitoredEntity", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *monitoredEntityServiceClient) FindMonitoredEntity(ctx context.Context, in *FindMonitoredEntityRequest, opts ...grpc.CallOption) (*FindMonitoredEntityRequest_Response, error) {
	out := new(FindMonitoredEntityRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.monitoring.MonitoredEntityService/findMonitoredEntity", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *monitoredEntityServiceClient) DeleteMonitoredEntity(ctx context.Context, in *DeleteMonitoredEntityRequest, opts ...grpc.CallOption) (*DeleteMonitoredEntityRequest_Response, error) {
	out := new(DeleteMonitoredEntityRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.monitoring.MonitoredEntityService/deleteMonitoredEntity", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// MonitoredEntityServiceServer is the server API for MonitoredEntityService service.
type MonitoredEntityServiceServer interface {
	CreateMonitoredEntity(context.Context, *CreateMonitoredEntityRequest) (*CreateMonitoredEntityRequest_Response, error)
	UpdateMonitoredEntity(context.Context, *UpdateMonitoredEntityRequest) (*UpdateMonitoredEntityRequest_Response, error)
	FindMonitoredEntity(context.Context, *FindMonitoredEntityRequest) (*FindMonitoredEntityRequest_Response, error)
	DeleteMonitoredEntity(context.Context, *DeleteMonitoredEntityRequest) (*DeleteMonitoredEntityRequest_Response, error)
}

// UnimplementedMonitoredEntityServiceServer can be embedded to have forward compatible implementations.
type UnimplementedMonitoredEntityServiceServer struct {
}

func (*UnimplementedMonitoredEntityServiceServer) CreateMonitoredEntity(context.Context, *CreateMonitoredEntityRequest) (*CreateMonitoredEntityRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CreateMonitoredEntity not implemented")
}
func (*UnimplementedMonitoredEntityServiceServer) UpdateMonitoredEntity(context.Context, *UpdateMonitoredEntityRequest) (*UpdateMonitoredEntityRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method UpdateMonitoredEntity not implemented")
}
func (*UnimplementedMonitoredEntityServiceServer) FindMonitoredEntity(context.Context, *FindMonitoredEntityRequest) (*FindMonitoredEntityRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method FindMonitoredEntity not implemented")
}
func (*UnimplementedMonitoredEntityServiceServer) DeleteMonitoredEntity(context.Context, *DeleteMonitoredEntityRequest) (*DeleteMonitoredEntityRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method DeleteMonitoredEntity not implemented")
}

func RegisterMonitoredEntityServiceServer(s *grpc.Server, srv MonitoredEntityServiceServer) {
	s.RegisterService(&_MonitoredEntityService_serviceDesc, srv)
}

func _MonitoredEntityService_CreateMonitoredEntity_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CreateMonitoredEntityRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(MonitoredEntityServiceServer).CreateMonitoredEntity(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.monitoring.MonitoredEntityService/CreateMonitoredEntity",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(MonitoredEntityServiceServer).CreateMonitoredEntity(ctx, req.(*CreateMonitoredEntityRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _MonitoredEntityService_UpdateMonitoredEntity_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(UpdateMonitoredEntityRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(MonitoredEntityServiceServer).UpdateMonitoredEntity(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.monitoring.MonitoredEntityService/UpdateMonitoredEntity",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(MonitoredEntityServiceServer).UpdateMonitoredEntity(ctx, req.(*UpdateMonitoredEntityRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _MonitoredEntityService_FindMonitoredEntity_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(FindMonitoredEntityRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(MonitoredEntityServiceServer).FindMonitoredEntity(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.monitoring.MonitoredEntityService/FindMonitoredEntity",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(MonitoredEntityServiceServer).FindMonitoredEntity(ctx, req.(*FindMonitoredEntityRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _MonitoredEntityService_DeleteMonitoredEntity_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(DeleteMonitoredEntityRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(MonitoredEntityServiceServer).DeleteMonitoredEntity(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.monitoring.MonitoredEntityService/DeleteMonitoredEntity",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(MonitoredEntityServiceServer).DeleteMonitoredEntity(ctx, req.(*DeleteMonitoredEntityRequest))
	}
	return interceptor(ctx, in, info, handler)
}

var _MonitoredEntityService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ai.verta.monitoring.MonitoredEntityService",
	HandlerType: (*MonitoredEntityServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "createMonitoredEntity",
			Handler:    _MonitoredEntityService_CreateMonitoredEntity_Handler,
		},
		{
			MethodName: "updateMonitoredEntity",
			Handler:    _MonitoredEntityService_UpdateMonitoredEntity_Handler,
		},
		{
			MethodName: "findMonitoredEntity",
			Handler:    _MonitoredEntityService_FindMonitoredEntity_Handler,
		},
		{
			MethodName: "deleteMonitoredEntity",
			Handler:    _MonitoredEntityService_DeleteMonitoredEntity_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "monitoring/MonitoredEntity.proto",
}
