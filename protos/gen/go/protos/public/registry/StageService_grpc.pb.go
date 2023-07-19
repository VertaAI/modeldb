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

// StageServiceClient is the client API for StageService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type StageServiceClient interface {
	// Anyone with RW permission on the model version can do any of these
	// Note that the author of the transition cannot approve or reject it themselves (like in github)
	CreateTransition(ctx context.Context, in *CreateTransitionRequest, opts ...grpc.CallOption) (*Activity, error)
	ApproveTransition(ctx context.Context, in *ApproveTransitionRequest, opts ...grpc.CallOption) (*Activity, error)
	RejectTransition(ctx context.Context, in *RejectTransitionRequest, opts ...grpc.CallOption) (*Activity, error)
	CloseTransition(ctx context.Context, in *CloseTransitionRequest, opts ...grpc.CallOption) (*Activity, error)
	CreateComment(ctx context.Context, in *CreateCommentRequest, opts ...grpc.CallOption) (*Activity, error)
	// Only a transition that has been approved can be commited. The user must have RW permission
	// Similar to merging a PR
	CommitTransition(ctx context.Context, in *CommitTransitionRequest, opts ...grpc.CallOption) (*Activity, error)
	// Directly update the stage without going through approval. The user must have RW permission
	// Similar to merging to master directly
	UpdateStage(ctx context.Context, in *UpdateStageRequest, opts ...grpc.CallOption) (*Activity, error)
	// List objects from the db. Similar to the PR history
	FindActivities(ctx context.Context, in *FindActivitiesRequest, opts ...grpc.CallOption) (*FindActivitiesRequest_Response, error)
	// Similar to listing PRs by state
	FindTransitions(ctx context.Context, in *FindTransitionsRequest, opts ...grpc.CallOption) (*FindTransitionsRequest_Response, error)
}

type stageServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewStageServiceClient(cc grpc.ClientConnInterface) StageServiceClient {
	return &stageServiceClient{cc}
}

func (c *stageServiceClient) CreateTransition(ctx context.Context, in *CreateTransitionRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/CreateTransition", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) ApproveTransition(ctx context.Context, in *ApproveTransitionRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/ApproveTransition", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) RejectTransition(ctx context.Context, in *RejectTransitionRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/RejectTransition", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) CloseTransition(ctx context.Context, in *CloseTransitionRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/CloseTransition", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) CreateComment(ctx context.Context, in *CreateCommentRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/CreateComment", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) CommitTransition(ctx context.Context, in *CommitTransitionRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/CommitTransition", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) UpdateStage(ctx context.Context, in *UpdateStageRequest, opts ...grpc.CallOption) (*Activity, error) {
	out := new(Activity)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/UpdateStage", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) FindActivities(ctx context.Context, in *FindActivitiesRequest, opts ...grpc.CallOption) (*FindActivitiesRequest_Response, error) {
	out := new(FindActivitiesRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/FindActivities", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *stageServiceClient) FindTransitions(ctx context.Context, in *FindTransitionsRequest, opts ...grpc.CallOption) (*FindTransitionsRequest_Response, error) {
	out := new(FindTransitionsRequest_Response)
	err := c.cc.Invoke(ctx, "/ai.verta.registry.StageService/FindTransitions", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// StageServiceServer is the server API for StageService service.
type StageServiceServer interface {
	// Anyone with RW permission on the model version can do any of these
	// Note that the author of the transition cannot approve or reject it themselves (like in github)
	CreateTransition(context.Context, *CreateTransitionRequest) (*Activity, error)
	ApproveTransition(context.Context, *ApproveTransitionRequest) (*Activity, error)
	RejectTransition(context.Context, *RejectTransitionRequest) (*Activity, error)
	CloseTransition(context.Context, *CloseTransitionRequest) (*Activity, error)
	CreateComment(context.Context, *CreateCommentRequest) (*Activity, error)
	// Only a transition that has been approved can be commited. The user must have RW permission
	// Similar to merging a PR
	CommitTransition(context.Context, *CommitTransitionRequest) (*Activity, error)
	// Directly update the stage without going through approval. The user must have RW permission
	// Similar to merging to master directly
	UpdateStage(context.Context, *UpdateStageRequest) (*Activity, error)
	// List objects from the db. Similar to the PR history
	FindActivities(context.Context, *FindActivitiesRequest) (*FindActivitiesRequest_Response, error)
	// Similar to listing PRs by state
	FindTransitions(context.Context, *FindTransitionsRequest) (*FindTransitionsRequest_Response, error)
}

// UnimplementedStageServiceServer can be embedded to have forward compatible implementations.
type UnimplementedStageServiceServer struct {
}

func (*UnimplementedStageServiceServer) CreateTransition(context.Context, *CreateTransitionRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CreateTransition not implemented")
}
func (*UnimplementedStageServiceServer) ApproveTransition(context.Context, *ApproveTransitionRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method ApproveTransition not implemented")
}
func (*UnimplementedStageServiceServer) RejectTransition(context.Context, *RejectTransitionRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method RejectTransition not implemented")
}
func (*UnimplementedStageServiceServer) CloseTransition(context.Context, *CloseTransitionRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CloseTransition not implemented")
}
func (*UnimplementedStageServiceServer) CreateComment(context.Context, *CreateCommentRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CreateComment not implemented")
}
func (*UnimplementedStageServiceServer) CommitTransition(context.Context, *CommitTransitionRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method CommitTransition not implemented")
}
func (*UnimplementedStageServiceServer) UpdateStage(context.Context, *UpdateStageRequest) (*Activity, error) {
	return nil, status.Errorf(codes.Unimplemented, "method UpdateStage not implemented")
}
func (*UnimplementedStageServiceServer) FindActivities(context.Context, *FindActivitiesRequest) (*FindActivitiesRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method FindActivities not implemented")
}
func (*UnimplementedStageServiceServer) FindTransitions(context.Context, *FindTransitionsRequest) (*FindTransitionsRequest_Response, error) {
	return nil, status.Errorf(codes.Unimplemented, "method FindTransitions not implemented")
}

func RegisterStageServiceServer(s *grpc.Server, srv StageServiceServer) {
	s.RegisterService(&_StageService_serviceDesc, srv)
}

func _StageService_CreateTransition_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CreateTransitionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).CreateTransition(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/CreateTransition",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).CreateTransition(ctx, req.(*CreateTransitionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_ApproveTransition_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(ApproveTransitionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).ApproveTransition(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/ApproveTransition",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).ApproveTransition(ctx, req.(*ApproveTransitionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_RejectTransition_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(RejectTransitionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).RejectTransition(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/RejectTransition",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).RejectTransition(ctx, req.(*RejectTransitionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_CloseTransition_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CloseTransitionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).CloseTransition(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/CloseTransition",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).CloseTransition(ctx, req.(*CloseTransitionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_CreateComment_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CreateCommentRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).CreateComment(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/CreateComment",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).CreateComment(ctx, req.(*CreateCommentRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_CommitTransition_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(CommitTransitionRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).CommitTransition(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/CommitTransition",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).CommitTransition(ctx, req.(*CommitTransitionRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_UpdateStage_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(UpdateStageRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).UpdateStage(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/UpdateStage",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).UpdateStage(ctx, req.(*UpdateStageRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_FindActivities_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(FindActivitiesRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).FindActivities(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/FindActivities",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).FindActivities(ctx, req.(*FindActivitiesRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _StageService_FindTransitions_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(FindTransitionsRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(StageServiceServer).FindTransitions(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/ai.verta.registry.StageService/FindTransitions",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(StageServiceServer).FindTransitions(ctx, req.(*FindTransitionsRequest))
	}
	return interceptor(ctx, in, info, handler)
}

var _StageService_serviceDesc = grpc.ServiceDesc{
	ServiceName: "ai.verta.registry.StageService",
	HandlerType: (*StageServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "CreateTransition",
			Handler:    _StageService_CreateTransition_Handler,
		},
		{
			MethodName: "ApproveTransition",
			Handler:    _StageService_ApproveTransition_Handler,
		},
		{
			MethodName: "RejectTransition",
			Handler:    _StageService_RejectTransition_Handler,
		},
		{
			MethodName: "CloseTransition",
			Handler:    _StageService_CloseTransition_Handler,
		},
		{
			MethodName: "CreateComment",
			Handler:    _StageService_CreateComment_Handler,
		},
		{
			MethodName: "CommitTransition",
			Handler:    _StageService_CommitTransition_Handler,
		},
		{
			MethodName: "UpdateStage",
			Handler:    _StageService_UpdateStage_Handler,
		},
		{
			MethodName: "FindActivities",
			Handler:    _StageService_FindActivities_Handler,
		},
		{
			MethodName: "FindTransitions",
			Handler:    _StageService_FindTransitions_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "registry/StageService.proto",
}
