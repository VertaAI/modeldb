package resolvers

import (
	"bytes"
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"github.com/gogo/protobuf/jsonpb"
	"go.uber.org/zap"
)

type commitResolver struct{ *Resolver }

func (r *commitResolver) ID(ctx context.Context, obj *models.Commit) (string, error) {
	return obj.Commit.GetCommitSha(), nil
}
func (r *commitResolver) Message(ctx context.Context, obj *models.Commit) (string, error) {
	return obj.Commit.Message, nil
}
func (r *commitResolver) Date(ctx context.Context, obj *models.Commit) (string, error) {
	return strconv.FormatUint(obj.Commit.GetDateCreated(), 10), nil
}
func (r *commitResolver) Author(ctx context.Context, obj *models.Commit) (*uac.UserInfo, error) {
	res, err := dataloaders.GetUserById(ctx, obj.Commit.GetAuthor())
	if err != nil {
		r.Logger.Error("failed to fetch author", zap.Error(err))
		return nil, err
	}
	return res, nil
}
func (r *commitResolver) GetLocation(ctx context.Context, obj *models.Commit, location []string) (schema.CommitElement, error) {
	res, err := r.Connections.Versioning.GetCommitComponent(ctx, &versioning.GetCommitComponentRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Repository.GetId(),
		},
		CommitSha: obj.Commit.GetCommitSha(),
		Location:  location,
	})
	if err != nil {
		r.Logger.Error("failed to get location", zap.Error(err))
		return nil, err
	}
	if folder := res.GetFolder(); folder != nil {
		res := &schema.CommitFolder{
			Subfolders: make([]*models.NamedCommitFolder, len(folder.GetSubFolders())),
			Blobs:      make([]*models.NamedCommitBlob, len(folder.GetBlobs())),
		}
		for i, blob := range folder.GetBlobs() {
			res.Blobs[i] = &models.NamedCommitBlob{
				Commit:   obj,
				Name:     blob.GetElementName(),
				Location: append(location, blob.GetElementName()),
			}
		}
		for i, subfolder := range folder.GetSubFolders() {
			res.Subfolders[i] = &models.NamedCommitFolder{
				Commit:   obj,
				Name:     subfolder.GetElementName(),
				Location: append(location, subfolder.GetElementName()),
			}
		}
		return res, nil
	} else if blob := res.GetBlob(); blob != nil {
		buffer := &bytes.Buffer{}
		if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(buffer, blob); err != nil {
			r.Logger.Error("failed to serialize blob", zap.Error(err))
			return nil, err
		}
		return &models.CommitBlob{
			Content:  buffer.String(),
			Commit:   obj,
			Location: location,
		}, nil
	} else {
		r.Logger.Error("unknown type for location", zap.Any("content", res))
		return nil, nil
	}
}
func (r *commitResolver) AsDiff(ctx context.Context, obj *models.Commit) (*schema.CommitAsDiff, error) {
	parents := obj.Commit.ParentShas
	if len(parents) == 0 {
		return nil, nil
	}
	parentReference := parents[0]
	diffs, err := r.Resolver.Repository().Diff(
		ctx,
		obj.Repository,
		schema.CommitReference{Commit: &parentReference},
		schema.CommitReference{Commit: &obj.Commit.CommitSha},
	)
	if err != nil {
		r.Logger.Error("failed to get diff", zap.Error(err))
		return nil, err
	}
	return &schema.CommitAsDiff{
		Parent: parentReference,
		Diff:   diffs,
	}, nil
}
func (r *commitResolver) Runs(ctx context.Context, obj *models.Commit, query *schema.ExperimentRunsQuery) (*schema.ExperimentRuns, error) {
	pagination := &common.Pagination{
		PageLimit:  10,
		PageNumber: 1,
	}
	if query != nil {
		qPagination := query.Pagination
		if qPagination != nil {
			if qPagination.Page != nil {
				pagination.PageNumber = int32(*qPagination.Page)
			}
			if qPagination.Limit != nil {
				pagination.PageLimit = int32(*qPagination.Limit)
			}
		}
	}

	res, err := r.Connections.ExperimentRun.ListCommitExperimentRuns(ctx, &modeldb.ListCommitExperimentRunsRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Repository.GetId(),
		},
		Pagination: pagination,
		CommitSha:  obj.Commit.GetCommitSha(),
	})
	if err != nil {
		r.Logger.Error("failed to load experiment runs", zap.Error(err))
		return nil, err
	}

	pageResponse := &schema.PaginationResponse{
		Page:         int(pagination.PageNumber + 1),
		Limit:        int(pagination.PageLimit),
		TotalRecords: int(res.GetTotalRecords()),
	}

	return &schema.ExperimentRuns{
		Runs:       res.GetRuns(),
		Pagination: pageResponse,
	}, nil
}
func (r *commitResolver) SetTag(ctx context.Context, obj *models.Commit, name string) (*versioning.Repository, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	_, err := r.Connections.Versioning.SetTag(ctx, &versioning.SetTagRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Repository.GetId(),
		},
		Tag:       name,
		CommitSha: obj.Commit.GetCommitSha(),
	})
	if err != nil {
		r.Logger.Error("failed to set tag", zap.Error(err))
		return nil, err
	}
	return obj.Repository, nil
}
func (r *commitResolver) SetBranch(ctx context.Context, obj *models.Commit, name string) (*versioning.Repository, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	_, err := r.Connections.Versioning.SetBranch(ctx, &versioning.SetBranchRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Repository.GetId(),
		},
		Branch:    name,
		CommitSha: obj.Commit.GetCommitSha(),
	})
	if err != nil {
		r.Logger.Error("failed to set branch", zap.Error(err))
		return nil, err
	}
	return obj.Repository, nil
}

type namedCommitBlobResolver struct{ *Resolver }

func (r *namedCommitBlobResolver) Content(ctx context.Context, obj *models.NamedCommitBlob) (*models.CommitBlob, error) {
	res, err := r.Resolver.Commit().GetLocation(ctx, obj.Commit, obj.Location)
	if err != nil {
		return nil, err
	}
	if blob, ok := res.(*models.CommitBlob); ok {
		return blob, nil
	}
	return nil, errors.InvalidTypeFromModeldb(ctx)
}

type namedCommitFolderResolver struct{ *Resolver }

func (r *namedCommitFolderResolver) Content(ctx context.Context, obj *models.NamedCommitFolder) (*schema.CommitFolder, error) {
	res, err := r.Resolver.Commit().GetLocation(ctx, obj.Commit, obj.Location)
	if err != nil {
		return nil, err
	}
	if folder, ok := res.(*schema.CommitFolder); ok {
		return folder, nil
	}
	return nil, errors.InvalidTypeFromModeldb(ctx)
}

type commitBlobResolver struct{ *Resolver }

func (r *commitBlobResolver) Runs(ctx context.Context, obj *models.CommitBlob, query *schema.ExperimentRunsQuery) (*schema.ExperimentRuns, error) {
	pagination := &common.Pagination{
		PageLimit:  10,
		PageNumber: 1,
	}
	if query != nil {
		qPagination := query.Pagination
		if qPagination != nil {
			if qPagination.Page != nil {
				pagination.PageNumber = int32(*qPagination.Page)
			}
			if qPagination.Limit != nil {
				pagination.PageLimit = int32(*qPagination.Limit)
			}
		}
	}

	res, err := r.Connections.ExperimentRun.ListBlobExperimentRuns(ctx, &modeldb.ListBlobExperimentRunsRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Commit.Repository.GetId(),
		},
		Pagination: pagination,
		CommitSha:  obj.Commit.Commit.GetCommitSha(),
		Location:   obj.Location,
	})
	if err != nil {
		r.Logger.Error("failed to load experiment runs", zap.Error(err))
		return nil, err
	}

	pageResponse := &schema.PaginationResponse{
		Page:         int(pagination.PageNumber + 1),
		Limit:        int(pagination.PageLimit),
		TotalRecords: int(res.GetTotalRecords()),
	}

	return &schema.ExperimentRuns{
		Runs:       res.GetRuns(),
		Pagination: pageResponse,
	}, nil
}
func (r *commitBlobResolver) DownloadURLForComponent(ctx context.Context, obj *models.CommitBlob, componentPath string) (string, error) {
	res, err := r.Connections.Versioning.GetUrlForBlobVersioned(ctx, &versioning.GetUrlForBlobVersioned{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Commit.Repository.GetId(),
		},
		CommitSha:                    obj.Commit.Commit.GetCommitSha(),
		Location:                     obj.Location,
		PathDatasetComponentBlobPath: componentPath,
		Method:                       "GET",
	})
	if err != nil {
		r.Logger.Error("failed to load url", zap.Error(err))
		return "", err
	}
	return res.GetUrl(), nil
}
