package resolvers

import (
	"context"

	"github.com/99designs/gqlgen/graphql"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	ai_verta_common "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
)

type mutationResolver struct{ *Resolver }

func (r *mutationResolver) EditRunDescription(ctx context.Context, id string, description string) (*ai_verta_modeldb.ExperimentRun, error) {
	res, err := r.Connections.ExperimentRun.UpdateExperimentRunDescription(
		ctx,
		&ai_verta_modeldb.UpdateExperimentRunDescription{Id: id, Description: description},
	)
	if err != nil {
		return nil, err
	}
	return res.GetExperimentRun(), nil
}
func (r *mutationResolver) AddRunTag(ctx context.Context, id string, tag string) (*ai_verta_modeldb.ExperimentRun, error) {
	res, err := r.Connections.ExperimentRun.AddExperimentRunTags(
		ctx,
		&ai_verta_modeldb.AddExperimentRunTags{Id: id, Tags: []string{tag}},
	)
	if err != nil {
		return nil, err
	}
	return res.GetExperimentRun(), nil
}
func (r *mutationResolver) DelRunTag(ctx context.Context, id string, tag string) (*ai_verta_modeldb.ExperimentRun, error) {
	res, err := r.Connections.ExperimentRun.DeleteExperimentRunTags(
		ctx,
		&ai_verta_modeldb.DeleteExperimentRunTags{Id: id, Tags: []string{tag}},
	)
	if err != nil {
		return nil, err
	}
	return res.GetExperimentRun(), nil
}
func (r *mutationResolver) SetCollaboratorProject(ctx context.Context, projid string, email string, typeArg schema.AccessType) (*ai_verta_modeldb.Project, error) {
	if r.Connections.HasUac() {
		t := ai_verta_common.CollaboratorTypeEnum_READ_ONLY
		switch typeArg {
		case schema.AccessTypeReadOnly:
			t = ai_verta_common.CollaboratorTypeEnum_READ_ONLY
		case schema.AccessTypeReadWrite:
			t = ai_verta_common.CollaboratorTypeEnum_READ_WRITE
		}

		res, err := r.Connections.Collaborator.AddOrUpdateProjectCollaborator(
			ctx,
			&ai_verta_uac.AddCollaboratorRequest{EntityIds: []string{projid}, ShareWith: email, CollaboratorType: t},
		)
		if err != nil {
			return nil, err
		}

		if !res.GetStatus() {
			return nil, errors.ModelDbInternalError(ctx)
		}
	}
	return r.Query().Project(ctx, projid)
}
func (r *mutationResolver) DelCollaboratorProject(ctx context.Context, projid string, collid string) (*ai_verta_modeldb.Project, error) {
	// TODO: figure out why we need the date deleted (empty doesn't delete)
	if r.Connections.HasUac() {
		res, err := r.Connections.Collaborator.RemoveProjectCollaborator(
			ctx,
			&ai_verta_uac.RemoveCollaborator{EntityId: projid, ShareWith: collid, DateDeleted: 1},
		)
		if err != nil {
			return nil, err
		}

		if !res.GetStatus() {
			return nil, errors.ModelDbInternalError(ctx)
		}
	}

	return r.Query().Project(ctx, projid)
}
func (r *mutationResolver) EditProjectDescription(ctx context.Context, id string, description string) (*ai_verta_modeldb.Project, error) {
	res, err := r.Connections.Project.UpdateProjectDescription(
		ctx,
		&ai_verta_modeldb.UpdateProjectDescription{Id: id, Description: description},
	)
	if err != nil {
		return nil, err
	}
	return res.GetProject(), nil
}
func (r *mutationResolver) EditProjectReadme(ctx context.Context, id string, readme string) (*ai_verta_modeldb.Project, error) {
	res, err := r.Connections.Project.SetProjectReadme(
		ctx,
		&ai_verta_modeldb.SetProjectReadme{Id: id, ReadmeText: readme},
	)
	if err != nil {
		return nil, err
	}
	return res.GetProject(), nil
}
func (r *mutationResolver) AddProjectTag(ctx context.Context, id string, tag string) (*ai_verta_modeldb.Project, error) {
	res, err := r.Connections.Project.AddProjectTag(
		ctx,
		&ai_verta_modeldb.AddProjectTag{Id: id, Tag: tag},
	)
	if err != nil {
		return nil, err
	}
	return res.GetProject(), nil
}
func (r *mutationResolver) DelProjectTag(ctx context.Context, id string, tag string) (*ai_verta_modeldb.Project, error) {
	res, err := r.Connections.Project.DeleteProjectTag(
		ctx,
		&ai_verta_modeldb.DeleteProjectTag{Id: id, Tag: tag},
	)
	if err != nil {
		return nil, err
	}
	return res.GetProject(), nil
}
func (r *mutationResolver) DelProject(ctx context.Context, id string) (bool, error) {
	res, err := r.Connections.Project.DeleteProject(
		ctx,
		&ai_verta_modeldb.DeleteProject{Id: id},
	)
	if err != nil {
		return false, err
	}
	return res.GetStatus(), nil
}
func (r *mutationResolver) Dataset(ctx context.Context, id string) (*modeldb.Dataset, error) {
	return r.Resolver.Query().Dataset(ctx, id)
}
func (r *mutationResolver) Repository(ctx context.Context, id string) (*versioning.Repository, error) {
	return r.Resolver.Query().Repository(ctx, id)
}
func (r *mutationResolver) Workspace(ctx context.Context, name *string) (*models.Workspace, error) {
	return r.Resolver.Query().Workspace(ctx, name)
}

func isMutation(ctx context.Context) bool {
	resolverContext := graphql.GetResolverContext(ctx)
	for resolverContext != nil {
		if resolverContext.Object == "Mutation" {
			return true
		}
		resolverContext = resolverContext.Parent
	}
	return false
}
