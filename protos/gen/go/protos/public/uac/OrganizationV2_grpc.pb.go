// Code generated by protoc-gen-go-grpc. DO NOT EDIT.

package uac

import (
	context "context"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
)

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion6

// OrganizationServiceV2Client is the client API for OrganizationServiceV2 service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type OrganizationServiceV2Client interface {
	// Gets information from a given organization
	GetOrganizationById(ctx context.Context, in *GetOrganizationByIdV2, opts ...grpc.CallOption) (*GetOrganizationByIdV2_Response, error)
	// Gets information from a given organization
	GetOrganizationByName(ctx context.Context, in *GetOrganizationByNameV2, opts ...grpc.CallOption) (*GetOrganizationByNameV2_Response, error)
	// Lists the organizations that the current user can access
	ListOrganizations(ctx context.Context, in *ListOrganizationsV2, opts ...grpc.CallOption) (*ListOrganizationsV2_Response, error)
	// Create or update an organization
	// Automatically sets the user making the call as owner and adds to the organization
	SetOrganization(ctx context.Context, in *SetOrganizationV2, opts ...grpc.CallOption) (*SetOrganizationV2_Response, error)
	// Delete an existing organization
	DeleteOrganization(ctx context.Context, in *DeleteOrganizationV2, opts ...grpc.CallOption) (*DeleteOrganizationV2_Response, error)
	CreateOrUpdateContainerRegistryConfiguration(ctx context.Context, in *ContainerRegistryConfiguration, opts ...grpc.CallOption) (*ContainerRegistryConfiguration, error)
	DeleteContainerRegistryConfiguration(ctx context.Context, in *ContainerRegistryConfiguration, opts ...grpc.CallOption) (*Empty, error)
}

type organizationServiceV2Client struct {
	cc grpc.ClientConnInterface
}

func NewOrganizationServiceV2Client(cc grpc.ClientConnInterface) OrganizationServiceV2Client {
	return &organizationServiceV2Client{cc}
}

func (c *organizationServiceV2Client) GetOrganizationById(ctx context.Context, in *GetOrganizationByIdV2, opts ...grpc.CallOption) (*GetOrganizationByIdV2_Response, error) {
	out := new(GetOrganizationByIdV2_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/getOrganizationById", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *organizationServiceV2Client) GetOrganizationByName(ctx context.Context, in *GetOrganizationByNameV2, opts ...grpc.CallOption) (*GetOrganizationByNameV2_Response, error) {
	out := new(GetOrganizationByNameV2_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/getOrganizationByName", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *organizationServiceV2Client) ListOrganizations(ctx context.Context, in *ListOrganizationsV2, opts ...grpc.CallOption) (*ListOrganizationsV2_Response, error) {
	out := new(ListOrganizationsV2_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/listOrganizations", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *organizationServiceV2Client) SetOrganization(ctx context.Context, in *SetOrganizationV2, opts ...grpc.CallOption) (*SetOrganizationV2_Response, error) {
	out := new(SetOrganizationV2_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/setOrganization", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *organizationServiceV2Client) DeleteOrganization(ctx context.Context, in *DeleteOrganizationV2, opts ...grpc.CallOption) (*DeleteOrganizationV2_Response, error) {
	out := new(DeleteOrganizationV2_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/deleteOrganization", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *organizationServiceV2Client) CreateOrUpdateContainerRegistryConfiguration(ctx context.Context, in *ContainerRegistryConfiguration, opts ...grpc.CallOption) (*ContainerRegistryConfiguration, error) {
	out := new(ContainerRegistryConfiguration)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/createOrUpdateContainerRegistryConfiguration", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *organizationServiceV2Client) DeleteContainerRegistryConfiguration(ctx context.Context, in *ContainerRegistryConfiguration, opts ...grpc.CallOption) (*Empty, error) {
	out := new(Empty)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.OrganizationServiceV2/deleteContainerRegistryConfiguration", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// OrganizationServiceV2Server is the server API for OrganizationServiceV2 service.
type OrganizationServiceV2Server interface {
	// Gets information from a given organization
	GetOrganizationById(context.Context, *GetOrganizationByIdV2) (*GetOrganizationByIdV2_Response, error)
	// Gets information from a given organization
	GetOrganizationByName(context.Context, *GetOrganizationByNameV2) (*GetOrganizationByNameV2_Response, error)
	// Lists the organizations that the current user can access
	ListOrganizations(context.Context, *ListOrganizationsV2) (*ListOrganizationsV2_Response, error)
	// Create or update an organization
	// Automatically sets the user making the call as owner and adds to the organization
	SetOrganization(context.Context, *SetOrganizationV2) (*SetOrganizationV2_Response, error)
	// Delete an existing organization
	DeleteOrganization(context.Context, *DeleteOrganizationV2) (*DeleteOrganizationV2_Response, error)
	CreateOrUpdateContainerRegistryConfiguration(context.Context, *ContainerRegistryConfiguration) (*ContainerRegistryConfiguration, error)
	DeleteContainerRegistryConfiguration(context.Context, *ContainerRegistryConfiguration) (*Empty, error)
}

// UnimplementedOrganizationServiceV2Server can be embedded to have forward compatible implementations.
type UnimplementedOrganizationServiceV2Server struct {
}

func (*UnimplementedOrganizationServiceV2Server) GetOrganizationById(context.Context, *GetOrganizationByIdV2) (*GetOrganizationByIdV2_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetOrganizationById not implemented")
}
func (*UnimplementedOrganizationServiceV2Server) GetOrganizationByName(context.Context, *GetOrganizationByNameV2) (*GetOrganizationByNameV2_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetOrganizationByName not implemented")
}
func (*UnimplementedOrganizationServiceV2Server) ListOrganizations(context.Context, *ListOrganizationsV2) (*ListOrganizationsV2_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method ListOrganizations not implemented")
}
func (*UnimplementedOrganizationServiceV2Server) SetOrganization(context.Context, *SetOrganizationV2) (*SetOrganizationV2_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method SetOrganization not implemented")
}
func (*UnimplementedOrganizationServiceV2Server) DeleteOrganization(context.Context, *DeleteOrganizationV2) (*DeleteOrganizationV2_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method DeleteOrganization not implemented")
}
func (*UnimplementedOrganizationServiceV2Server) CreateOrUpdateContainerRegistryConfiguration(context.Context, *ContainerRegistryConfiguration) (*ContainerRegistryConfiguration, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CreateOrUpdateContainerRegistryConfiguration not implemented")
}
func (*UnimplementedOrganizationServiceV2Server) DeleteContainerRegistryConfiguration(context.Context, *ContainerRegistryConfiguration) (*Empty, error) {
	return nil, status.Errorf(codes.Unimplemented, "method DeleteContainerRegistryConfiguration not implemented")
}

func RegisterOrganizationServiceV2Server(s *grpc.Server, srv OrganizationServiceV2Server) {
	s.RegisterService(&_OrganizationServiceV2_serviceDesc, srv)
}

func _OrganizationServiceV2_GetOrganizationById_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(GetOrganizationByIdV2)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).GetOrganizationById(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/GetOrganizationById",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).GetOrganizationById(ctx, req.(*GetOrganizationByIdV2))
	}
	return interceptor(ctx, in, info, handler)
}

func _OrganizationServiceV2_GetOrganizationByName_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(GetOrganizationByNameV2)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).GetOrganizationByName(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/GetOrganizationByName",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).GetOrganizationByName(ctx, req.(*GetOrganizationByNameV2))
	}
	return interceptor(ctx, in, info, handler)
}

func _OrganizationServiceV2_ListOrganizations_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(ListOrganizationsV2)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).ListOrganizations(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/ListOrganizations",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).ListOrganizations(ctx, req.(*ListOrganizationsV2))
	}
	return interceptor(ctx, in, info, handler)
}

func _OrganizationServiceV2_SetOrganization_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(SetOrganizationV2)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).SetOrganization(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/SetOrganization",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).SetOrganization(ctx, req.(*SetOrganizationV2))
	}
	return interceptor(ctx, in, info, handler)
}

func _OrganizationServiceV2_DeleteOrganization_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(DeleteOrganizationV2)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).DeleteOrganization(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/DeleteOrganization",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).DeleteOrganization(ctx, req.(*DeleteOrganizationV2))
	}
	return interceptor(ctx, in, info, handler)
}

func _OrganizationServiceV2_CreateOrUpdateContainerRegistryConfiguration_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(ContainerRegistryConfiguration)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).CreateOrUpdateContainerRegistryConfiguration(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/CreateOrUpdateContainerRegistryConfiguration",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).CreateOrUpdateContainerRegistryConfiguration(ctx, req.(*ContainerRegistryConfiguration))
	}
	return interceptor(ctx, in, info, handler)
}

func _OrganizationServiceV2_DeleteContainerRegistryConfiguration_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(ContainerRegistryConfiguration)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(OrganizationServiceV2Server).DeleteContainerRegistryConfiguration(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.OrganizationServiceV2/DeleteContainerRegistryConfiguration",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(OrganizationServiceV2Server).DeleteContainerRegistryConfiguration(ctx, req.(*ContainerRegistryConfiguration))
	}
	return interceptor(ctx, in, info, handler)
}

var _OrganizationServiceV2_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ai.verta.uac.OrganizationServiceV2",
	HandlerType: (*OrganizationServiceV2Server)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "getOrganizationById",
			Handler:    _OrganizationServiceV2_GetOrganizationById_Handler,
		},
		{
			MethodName: "getOrganizationByName",
			Handler:    _OrganizationServiceV2_GetOrganizationByName_Handler,
		},
		{
			MethodName: "listOrganizations",
			Handler:    _OrganizationServiceV2_ListOrganizations_Handler,
		},
		{
			MethodName: "setOrganization",
			Handler:    _OrganizationServiceV2_SetOrganization_Handler,
		},
		{
			MethodName: "deleteOrganization",
			Handler:    _OrganizationServiceV2_DeleteOrganization_Handler,
		},
		{
			MethodName: "createOrUpdateContainerRegistryConfiguration",
			Handler:    _OrganizationServiceV2_CreateOrUpdateContainerRegistryConfiguration_Handler,
		},
		{
			MethodName: "deleteContainerRegistryConfiguration",
			Handler:    _OrganizationServiceV2_DeleteContainerRegistryConfiguration_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "uac/OrganizationV2.proto",
}
