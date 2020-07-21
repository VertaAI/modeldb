package resolvers

import (
	"context"
	"fmt"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/pagination"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
)

type workspaceResolver struct{ *Resolver }

func (r *Resolver) resolveWorkspaceName(ctx context.Context, workspaceID string, workspaceType common.WorkspaceTypeEnum_WorkspaceType) (*string, error) {
	var workspaceName *string
	switch workspaceType {
	case common.WorkspaceTypeEnum_USER:
		response, err := r.Connections.UAC.GetUser(ctx, &uac.GetUser{
			UserId: workspaceID,
		})
		if err != nil {
			r.Logger.Error("failed to get workspace name from user", zap.Error(err))
			return nil, err
		}
		workspaceName = &response.VertaInfo.Username
	case common.WorkspaceTypeEnum_ORGANIZATION:
		response, err := r.Connections.Organization.GetOrganizationById(ctx, &uac.GetOrganizationById{
			OrgId: workspaceID,
		})
		if err != nil {
			r.Logger.Error("failed to get workspace name from organization", zap.Error(err))
			return nil, err
		}
		workspaceName = &response.Organization.Name
	default:
		r.Logger.Error(fmt.Sprintf("Unknown workspace type %s", workspaceType))
		return nil, errors.UnknownWorkspaceType(ctx, workspaceType)
	}
	return workspaceName, nil
}
func (r *workspaceResolver) Projects(ctx context.Context, obj *models.Workspace, next *string, query *schema.ProjectsQuery) (*schema.Projects, error) {
	var pageQuery *schema.PaginationQuery
	if query != nil {
		pageQuery = query.Pagination
	}

	nextObj, err := pagination.NewNext(r.Logger, ctx, next, pageQuery)
	if err != nil {
		return nil, err
	}

	res, err := r.Connections.Project.GetProjects(ctx, &modeldb.GetProjects{
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

func (r *workspaceResolver) Datasets(ctx context.Context, obj *models.Workspace, query *schema.DatasetsQuery) (*schema.Datasets, error) {
	var pageQuery *schema.PaginationQuery
	var ids []string
	var predicates []*common.KeyValueQuery
	if query != nil {
		pageQuery = query.Pagination

		ids = query.Ids

		var err error
		predicates, err = r.resolveMDBPredicates(ctx, query.StringPredicates, query.FloatPredicates)
		if err != nil {
			r.Logger.Error("failed to resolve predicates", zap.Error(err))
			return nil, err
		}
	}

	nextObj, err := pagination.NewNext(r.Logger, ctx, nil, pageQuery)
	if err != nil {
		return nil, err
	}

	res, err := r.Connections.Dataset.FindDatasets(ctx, &modeldb.FindDatasets{
		DatasetIds:    ids,
		WorkspaceName: obj.Name,
		Predicates:    predicates,
		PageNumber:    int32(nextObj.PageNumber),
		PageLimit:     int32(nextObj.PageLimit),
	})
	if err != nil {
		r.Logger.Error("failed to get datasets", zap.Error(err))
		return nil, err
	}

	nextObj.ProcessResponse(int64(res.GetTotalRecords()))

	return &schema.Datasets{
		Datasets: res.GetDatasets(),
		Total:    int(res.GetTotalRecords()),
	}, nil
}
func (r *workspaceResolver) DatasetVersions(ctx context.Context, obj *models.Workspace, query *schema.DatasetVersionsQuery) (*schema.DatasetVersions, error) {
	var pageQuery *schema.PaginationQuery
	var ids []string
	var predicates []*common.KeyValueQuery
	if query != nil {
		pageQuery = query.Pagination

		ids = query.Ids

		var err error
		predicates, err = r.resolveMDBPredicates(ctx, query.StringPredicates, query.FloatPredicates)
		if err != nil {
			r.Logger.Error("failed to resolve predicates", zap.Error(err))
			return nil, err
		}
	}

	nextObj, err := pagination.NewNext(r.Logger, ctx, nil, pageQuery)
	if err != nil {
		return nil, err
	}

	res, err := r.Connections.DatasetVersion.FindDatasetVersions(ctx, &modeldb.FindDatasetVersions{
		DatasetVersionIds: ids,
		WorkspaceName:     obj.Name,
		Predicates:        predicates,
		PageNumber:        int32(nextObj.PageNumber),
		PageLimit:         int32(nextObj.PageLimit),
	})
	if err != nil {
		r.Logger.Error("failed to get dataset versions", zap.Error(err))
		return nil, err
	}

	nextObj.ProcessResponse(int64(res.GetTotalRecords()))

	return &schema.DatasetVersions{
		DatasetVersions: res.GetDatasetVersions(),
		Total:           int(res.GetTotalRecords()),
	}, nil
}
func (r *workspaceResolver) CreateDataset(ctx context.Context, obj *models.Workspace, name string, visibility schema.Visibility) (*modeldb.Dataset, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.CreateOutsideMutation(ctx).Message)
		return nil, errors.CreateOutsideMutation(ctx)
	}
	datasetVisibility := modeldb.DatasetVisibilityEnum_PRIVATE
	switch visibility {
	case schema.VisibilityPrivate:
		datasetVisibility = modeldb.DatasetVisibilityEnum_PRIVATE
	case schema.VisibilityOrgScopedPublic:
		datasetVisibility = modeldb.DatasetVisibilityEnum_ORG_SCOPED_PUBLIC
	case schema.VisibilityPublic:
		datasetVisibility = modeldb.DatasetVisibilityEnum_PUBLIC
	}
	res, err := r.Connections.Dataset.CreateDataset(ctx, &modeldb.CreateDataset{
		Name:              name,
		WorkspaceName:     obj.Name,
		DatasetVisibility: datasetVisibility,
	})
	if err != nil {
		r.Logger.Error("failed to create dataset", zap.Error(err))
		return nil, err
	}
	return res.GetDataset(), nil
}

func (r *workspaceResolver) Repositories(ctx context.Context, obj *models.Workspace, next *string, query *schema.RepositoriesQuery) (*schema.Repositories, error) {
	var pageQuery *schema.PaginationQuery
	var ids []uint64
	var predicates []*common.KeyValueQuery

	if query != nil {
		pageQuery = query.Pagination

		ids = make([]uint64, len(query.Ids))
		for i, id := range query.Ids {
			ids[i] = uint64(id)
		}

		var err error
		predicates, err = r.resolveMDBPredicates(ctx, query.StringPredicates, query.FloatPredicates)
		if err != nil {
			r.Logger.Error("failed to resolve predicates", zap.Error(err))
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

// TODO: Add/expose sorting and order
