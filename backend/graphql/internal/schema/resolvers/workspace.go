package resolvers

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/pagination"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	"go.uber.org/zap"
)

type workspaceResolver struct{ *Resolver }

func (r *workspaceResolver) Projects(ctx context.Context, obj *models.Workspace, next *string, query *schema.ProjectsQuery) (*schema.Projects, error) {
	var pageQuery *schema.PaginationQuery
	if query != nil {
		pageQuery = query.Pagination
	}

	nextObj, err := pagination.NewNext(r.Logger, ctx, next, pageQuery)
	if err != nil {
		return nil, err
	}

	res, err := r.Connections.Project.GetProjects(ctx, &ai_verta_modeldb.GetProjects{
		PageNumber:    int32(nextObj.PageNumber),
		PageLimit:     int32(nextObj.PageLimit),
		WorkspaceName: obj.Name,
	})
	if err != nil {
		r.Logger.Error("failed to get projects", zap.Error(err))
		return nil, err
	}

	nextObj.ProcessResponse(res.GetTotalRecords())

	return &schema.Projects{
		Projects: res.GetProjects(),
		Next:     nextObj.Encode(),
	}, nil
}

func (r *workspaceResolver) Repositories(ctx context.Context, obj *models.Workspace, next *string, query *schema.RepositoriesQuery) (*schema.Repositories, error) {
	var pageQuery *schema.PaginationQuery
	var ids []uint64
	var predicates []*modeldb.KeyValueQuery

	if query != nil {
		pageQuery = query.Pagination

		ids = make([]uint64, len(query.Ids))
		for i, id := range query.Ids {
			ids[i] = uint64(id)
		}

		var err error
		predicates, err = r.resolveMDBPredicates(ctx, query.StringPredicates, query.FloatPredicates)
		if err != nil {
			return nil, err
		}
	}

	nextObj, err := pagination.NewNext(r.Logger, ctx, next, pageQuery)
	if err != nil {
		return nil, err
	}

	res, err := r.Connections.Versioning.FindRepositories(ctx, &versioning.FindRepositories{
		RepoIds:       ids,
		WorkspaceName: obj.Name,
		Predicates:    predicates,
		PageNumber:    int32(nextObj.PageNumber),
		PageLimit:     int32(nextObj.PageLimit),
	})
	if err != nil {
		r.Logger.Error("failed to get repositories", zap.Error(err))
		return nil, err
	}

	totalRecords := int(res.GetTotalRecords())
	pageResponse := &schema.PaginationResponse{
		Page:         nextObj.PageNumber,
		Limit:        nextObj.PageLimit,
		TotalRecords: totalRecords,
	}
	nextObj.ProcessResponse(res.GetTotalRecords())

	return &schema.Repositories{
		Repositories: res.GetRepositories(),
		Next:         nextObj.Encode(),
		Pagination:   pageResponse,
	}, nil
}

func (r *workspaceResolver) Repository(ctx context.Context, obj *models.Workspace, name string) (*versioning.Repository, error) {
	res, err := r.Connections.Versioning.GetRepository(
		ctx,
		&versioning.GetRepositoryRequest{Id: &versioning.RepositoryIdentification{
			NamedId: &versioning.RepositoryNamedIdentification{
				Name:          name,
				WorkspaceName: obj.Name,
			},
		}},
	)
	if err != nil {
		r.Logger.Error("failed to get repository", zap.Error(err))
		return nil, err
	}
	return res.GetRepository(), nil
}

func (r *workspaceResolver) CreateRepository(ctx context.Context, obj *models.Workspace, name string, visibility schema.Visibility) (*versioning.Repository, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.CreateOutsideMutation(ctx).Message)
		return nil, errors.CreateOutsideMutation(ctx)
	}
	repoVisibility := versioning.RepositoryVisibilityEnum_PRIVATE
	switch visibility {
	case schema.VisibilityPrivate:
		repoVisibility = versioning.RepositoryVisibilityEnum_PRIVATE
	case schema.VisibilityOrgScopedPublic:
		repoVisibility = versioning.RepositoryVisibilityEnum_ORG_SCOPED_PUBLIC
	case schema.VisibilityPublic:
		repoVisibility = versioning.RepositoryVisibilityEnum_PUBLIC
	}
	res, err := r.Connections.Versioning.CreateRepository(ctx, &versioning.SetRepository{
		Id: &versioning.RepositoryIdentification{
			NamedId: &versioning.RepositoryNamedIdentification{
				Name:          name,
				WorkspaceName: obj.Name,
			},
		},
		Repository: &versioning.Repository{
			Name:                 name,
			RepositoryVisibility: repoVisibility,
		},
	})
	if err != nil {
		r.Logger.Error("failed to create repository", zap.Error(err))
		return nil, err
	}
	return res.GetRepository(), nil
}
