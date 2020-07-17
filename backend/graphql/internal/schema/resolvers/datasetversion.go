package resolvers

import (
	"bytes"
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"github.com/gogo/protobuf/jsonpb"
	"go.uber.org/zap"
)

type datasetVersionResolver struct{ *Resolver }

func (r *datasetVersionResolver) Dataset(ctx context.Context, obj *modeldb.DatasetVersion) (*modeldb.Dataset, error) {
	return r.Query().Dataset(ctx, obj.GetDatasetId())
}
func (r *datasetVersionResolver) DateCreated(ctx context.Context, obj *modeldb.DatasetVersion) (string, error) {
	return strconv.FormatUint(obj.GetTimeLogged(), 10), nil
}
func (r *datasetVersionResolver) DateUpdated(ctx context.Context, obj *modeldb.DatasetVersion) (string, error) {
	return strconv.FormatUint(obj.GetTimeUpdated(), 10), nil
}
func (r *datasetVersionResolver) Attributes(ctx context.Context, obj *modeldb.DatasetVersion) ([]schema.KeyValue, error) {
	res, err := keyValueSliceConverter(ctx, obj.GetAttributes())
	if err != nil {
		r.Logger.Error("Failed to slice keyvalues", zap.Error(err))
		return nil, err
	}
	return res, nil
}
func (r *datasetVersionResolver) Owner(ctx context.Context, obj *modeldb.DatasetVersion) (*uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwner())
}
func (r *datasetVersionResolver) Version(ctx context.Context, obj *modeldb.DatasetVersion) (int, error) {
	return int(obj.GetVersion()), nil
}
func (r *datasetVersionResolver) AddTags(ctx context.Context, obj *modeldb.DatasetVersion, tags []string) (*modeldb.DatasetVersion, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	response, err := r.Connections.DatasetVersion.AddDatasetVersionTags(ctx, &modeldb.AddDatasetVersionTags{
		Id:   obj.GetId(),
		Tags: tags,
	})

	if err != nil {
		r.Logger.Error("failed to add tags", zap.Error(err))
		return nil, err
	}

	return response.DatasetVersion, nil
}
func (r *datasetVersionResolver) DeleteTags(ctx context.Context, obj *modeldb.DatasetVersion, tags []string) (*modeldb.DatasetVersion, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	response, err := r.Connections.DatasetVersion.DeleteDatasetVersionTags(ctx, &modeldb.DeleteDatasetVersionTags{
		Id:   obj.GetId(),
		Tags: tags,
	})
	if err != nil {
		r.Logger.Error("failed to delete tags", zap.Error(err))
		return nil, err
	}

	return response.DatasetVersion, nil
}
func (r *datasetVersionResolver) Runs(ctx context.Context, obj *modeldb.DatasetVersion, query *schema.ExperimentRunsQuery) (*schema.ExperimentRuns, error) {
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

	res, err := r.Connections.ExperimentRun.GetExperimentRunsByDatasetVersionId(ctx, &modeldb.GetExperimentRunsByDatasetVersionId{
		DatasetVersionId: obj.GetId(),
		PageNumber:       pagination.PageNumber,
		PageLimit:        pagination.PageLimit,
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
		Runs:       res.GetExperimentRuns(),
		Pagination: pageResponse,
	}, nil
}
func (r *datasetVersionResolver) BlobInfo(ctx context.Context, obj *modeldb.DatasetVersion) (*string, error) {
	info := obj.GetDatasetBlob()
	if info == nil {
		return nil, nil
	}

	var buffer bytes.Buffer
	if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(&buffer, info); err != nil {
		r.Logger.Error("failed to serialize info", zap.Error(err))
		return nil, err
	}

	s := buffer.String()
	return &s, nil
}
func (r *datasetVersionResolver) RawInfo(ctx context.Context, obj *modeldb.DatasetVersion) (*string, error) {
	info := obj.GetRawDatasetVersionInfo()
	if info == nil {
		return nil, nil
	}

	var buffer bytes.Buffer
	if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(&buffer, info); err != nil {
		r.Logger.Error("failed to serialize info", zap.Error(err))
		return nil, err
	}

	s := buffer.String()
	return &s, nil
}
func (r *datasetVersionResolver) PathInfo(ctx context.Context, obj *modeldb.DatasetVersion) (*string, error) {
	info := obj.GetPathDatasetVersionInfo()
	if info == nil {
		return nil, nil
	}

	var buffer bytes.Buffer
	if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(&buffer, info); err != nil {
		r.Logger.Error("failed to serialize info", zap.Error(err))
		return nil, err
	}

	s := buffer.String()
	return &s, nil
}
func (r *datasetVersionResolver) QueryInfo(ctx context.Context, obj *modeldb.DatasetVersion) (*string, error) {
	info := obj.GetQueryDatasetVersionInfo()
	if info == nil {
		return nil, nil
	}

	var buffer bytes.Buffer
	if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(&buffer, info); err != nil {
		r.Logger.Error("failed to serialize info", zap.Error(err))
		return nil, err
	}

	s := buffer.String()
	return &s, nil
}
func (r *datasetVersionResolver) DownloadURL(ctx context.Context, obj *modeldb.DatasetVersion, blobPath string) (*string, error) {
	dataset, err := r.Resolver.Query().Dataset(ctx, obj.GetDatasetId())
	if err != nil {
		r.Logger.Error("Failed to get dataset", zap.Error(err))
		return nil, err
	}

	workspaceName, err := r.Resolver.resolveWorkspaceName(ctx, dataset.WorkspaceId, dataset.WorkspaceType)
	if err != nil {
		return nil, err
	}

	response, err := r.Connections.DatasetVersion.GetUrlForDatasetBlobVersioned(ctx, &modeldb.GetUrlForDatasetBlobVersioned{
		DatasetId:                    obj.DatasetId,
		DatasetVersionId:             obj.Id,
		Method:                       "GET",
		PathDatasetComponentBlobPath: blobPath,
		WorkspaceName:                *workspaceName,
	})

	if err != nil {
		r.Logger.Error("Failed to get url for dataset version", zap.Error(err))
		return nil, err
	}
	return &response.Url, nil
}
func (r *datasetVersionResolver) ChangeDescription(ctx context.Context, obj *modeldb.DatasetVersion, description string) (*modeldb.DatasetVersion, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}

	response, err := r.Connections.DatasetVersion.UpdateDatasetVersionDescription(ctx, &modeldb.UpdateDatasetVersionDescription{
		Description: description,
		Id:          obj.GetId(),
	})

	if err != nil {
		r.Logger.Error("failed to change dataset version description", zap.Error(err))
		return nil, err
	}

	return response.DatasetVersion, nil
}

func (r *datasetVersionResolver) Delete(ctx context.Context, obj *modeldb.DatasetVersion) (bool, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.DeleteOutsideMutation(ctx).Message)
		return false, errors.DeleteOutsideMutation(ctx)
	}

	_, err := r.Connections.DatasetVersion.DeleteDatasetVersion(ctx, &modeldb.DeleteDatasetVersion{
		Id:        obj.GetId(),
		DatasetId: obj.GetDatasetId(),
	})

	if err != nil {
		r.Logger.Error("failed to delete dataset version", zap.Error(err))
		return false, err
	}

	return true, nil
}
