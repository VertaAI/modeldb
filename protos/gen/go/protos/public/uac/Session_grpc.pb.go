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

// SessionServiceClient is the client API for SessionService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type SessionServiceClient interface {
	CreateSession(ctx context.Context, in *CreateSessionRequest, opts ...grpc.CallOption) (*Session, error)
	FindSession(ctx context.Context, in *FindSessionRequest, opts ...grpc.CallOption) (*FindSessionRequest_Response, error)
	DeleteSession(ctx context.Context, in *DeleteSessionRequest, opts ...grpc.CallOption) (*Empty, error)
}

type sessionServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewSessionServiceClient(cc grpc.ClientConnInterface) SessionServiceClient {
	return &sessionServiceClient{cc}
}

func (c *sessionServiceClient) CreateSession(ctx context.Context, in *CreateSessionRequest, opts ...grpc.CallOption) (*Session, error) {
	out := new(Session)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.SessionService/createSession", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *sessionServiceClient) FindSession(ctx context.Context, in *FindSessionRequest, opts ...grpc.CallOption) (*FindSessionRequest_Response, error) {
	out := new(FindSessionRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.SessionService/findSession", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *sessionServiceClient) DeleteSession(ctx context.Context, in *DeleteSessionRequest, opts ...grpc.CallOption) (*Empty, error) {
	out := new(Empty)
	err := c.cc.Invoke(ctx, "/ai.verta.uac.SessionService/deleteSession", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// SessionServiceServer is the server API for SessionService service.
type SessionServiceServer interface {
	CreateSession(context.Context, *CreateSessionRequest) (*Session, error)
	FindSession(context.Context, *FindSessionRequest) (*FindSessionRequest_Response, error)
	DeleteSession(context.Context, *DeleteSessionRequest) (*Empty, error)
}

// UnimplementedSessionServiceServer can be embedded to have forward compatible implementations.
type UnimplementedSessionServiceServer struct {
}

func (*UnimplementedSessionServiceServer) CreateSession(context.Context, *CreateSessionRequest) (*Session, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CreateSession not implemented")
}
func (*UnimplementedSessionServiceServer) FindSession(context.Context, *FindSessionRequest) (*FindSessionRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method FindSession not implemented")
}
func (*UnimplementedSessionServiceServer) DeleteSession(context.Context, *DeleteSessionRequest) (*Empty, error) {
	return nil, status.Errorf(codes.Unimplemented, "method DeleteSession not implemented")
}

func RegisterSessionServiceServer(s *grpc.Server, srv SessionServiceServer) {
	s.RegisterService(&_SessionService_serviceDesc, srv)
}

func _SessionService_CreateSession_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CreateSessionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SessionServiceServer).CreateSession(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.SessionService/CreateSession",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SessionServiceServer).CreateSession(ctx, req.(*CreateSessionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _SessionService_FindSession_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(FindSessionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SessionServiceServer).FindSession(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.SessionService/FindSession",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SessionServiceServer).FindSession(ctx, req.(*FindSessionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _SessionService_DeleteSession_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(DeleteSessionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SessionServiceServer).DeleteSession(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.uac.SessionService/DeleteSession",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SessionServiceServer).DeleteSession(ctx, req.(*DeleteSessionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

var _SessionService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ai.verta.uac.SessionService",
	HandlerType: (*SessionServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "createSession",
			Handler:    _SessionService_CreateSession_Handler,
		},
		{
			MethodName: "findSession",
			Handler:    _SessionService_FindSession_Handler,
		},
		{
			MethodName: "deleteSession",
			Handler:    _SessionService_DeleteSession_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "uac/Session.proto",
}
