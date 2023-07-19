// Code generated by protoc-gen-go-grpc. DO NOT EDIT.

package registry

import (
	context "context"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
)

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// DashboardServiceClient is the client API for DashboardService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
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
