package resolvers

import (
	"bytes"
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	pcommon "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/metadata"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"github.com/gogo/protobuf/jsonpb"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type repositoryResolver struct{ *Resolver }

func (r *repositoryResolver) ID(ctx context.Context, obj *versioning.Repository) (string, error) {
	return r.id(obj), nil
}

func (r *repositoryResolver) id(obj *versioning.Repository) string {
	return strconv.FormatUint(obj.GetId(), 10)
}

func (r *repositoryResolver) DateCreated(ctx context.Context, obj *versioning.Repository) (string, error) {
	return strconv.FormatUint(obj.GetDateCreated(), 10), nil
}

func (r *repositoryResolver) DateUpdated(ctx context.Context, obj *versioning.Repository) (string, error) {
	return strconv.FormatUint(obj.GetDateUpdated(), 10), nil
}

func (r *repositoryResolver) Labels(ctx context.Context, obj *versioning.Repository) ([]string, error) {
	res, err := r.Resolver.Connections.Metadata.GetLabels(ctx, &metadata.GetLabelsRequest{
		Id: &metadata.IdentificationType{
			IdType: metadata.IDTypeEnum_VERSIONING_REPOSITORY,
			Id:     &metadata.IdentificationType_IntId{IntId: obj.Id},
		},
	})
	if err != nil {
		r.Logger.Error("failed to get labels", zap.Error(err))
		return nil, err
	}
	return res.Labels, nil
}

func (r *repositoryResolver) Owner(ctx context.Context, obj *versioning.Repository) (*uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwner())
}

func (r *repositoryResolver) Collaborators(ctx context.Context, obj *versioning.Repository) ([]schema.Collaborator, error) {
	if r.Connections.HasUac() {
		return getConvertedCollaborators(r.Resolver, ctx, r.id(obj), func(ctx context.Context, in *uac.GetCollaborator, opts ...grpc.CallOption) (*uac.GetCollaborator_Response, error) {
			return r.Connections.Collaborator.GetRepositoryCollaborators(ctx, in, opts...)
		})
	}
	return []schema.Collaborator{}, nil
}
func (r *repositoryResolver) AllowedActions(ctx context.Context, obj *versioning.Repository) (*schema.AllowedActions, error) {
	if r.Connections.HasUac() {
		id := r.id(obj)
		res, err := r.Connections.Authorization.GetSelfAllowedActionsBatch(ctx, &uac.GetSelfAllowedActionsBatch{
			Resources: &uac.Resources{
				Service:     uac.ServiceEnum_MODELDB_SERVICE,
				ResourceIds: []string{id},
				ResourceType: &uac.ResourceType{
					Resource: &uac.ResourceType_ModeldbServiceResourceType{
						ModeldbServiceResourceType: pcommon.ModelDBResourceEnum_REPOSITORY,
					},
				},
			},
		})
		if err != nil {
			r.Logger.Error("failed to get allowed actions", zap.Error(err))
			return nil, err
		}

		var ret schema.AllowedActions

		if actions, ok := res.GetActions()[id]; ok {
			for _, act := range actions.GetActions() {
				switch act.GetModeldbServiceAction() {
				case uac.ModelDBActionEnum_CREATE:
					ret.Create = true
				case uac.ModelDBActionEnum_DELETE:
					ret.Delete = true
				case uac.ModelDBActionEnum_UPDATE:
					ret.Update = true
				case uac.ModelDBActionEnum_DEPLOY:
					ret.Deploy = true
				}
			}
		}

		return &ret, nil
	}
	return &schema.AllowedActions{
		Create: true,
		Update: true,
		Delete: true,
	}, nil
}
func (r *repositoryResolver) Tags(ctx context.Context, obj *versioning.Repository) ([]*models.RepositoryTag, error) {
	res, err := r.Connections.Versioning.ListTags(ctx, &versioning.ListTagsRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
	})
	if err != nil {
		r.Logger.Error("failed to list tags", zap.Error(err))
		return nil, err
	}

	response := make([]*models.RepositoryTag, len(res.GetTags()))
	for i, tag := range res.GetTags() {
		response[i] = &models.RepositoryTag{
			Repository: obj,
			Name:       tag,
		}
	}
	return response, nil
}
func (r *repositoryResolver) Branches(ctx context.Context, obj *versioning.Repository) ([]*models.RepositoryBranch, error) {
	res, err := r.Connections.Versioning.ListBranches(ctx, &versioning.ListBranchesRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
	})
	if err != nil {
		r.Logger.Error("failed to list branches", zap.Error(err))
		return nil, err
	}

	response := make([]*models.RepositoryBranch, len(res.GetBranches()))
	for i, tag := range res.GetBranches() {
		response[i] = &models.RepositoryBranch{
			Repository: obj,
			Name:       tag,
		}
	}
	return response, nil
}
func (r *repositoryResolver) Commit(ctx context.Context, obj *versioning.Repository, id string) (*models.Commit, error) {
	res, err := r.Connections.Versioning.GetCommit(ctx, &versioning.GetCommitRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
		CommitSha: id,
	})
	if err != nil {
		r.Logger.Error("failed to get commit", zap.Error(err))
		return nil, err
	}
	return &models.Commit{
		Repository: obj,
		Commit:     res.GetCommit(),
	}, nil
}
func (r *repositoryResolver) Tag(ctx context.Context, obj *versioning.Repository, name string) (*models.Commit, error) {
	return r.Resolver.RepositoryTag().Commit(ctx, &models.RepositoryTag{
		Repository: obj,
		Name:       name,
	})
}
func (r *repositoryResolver) Branch(ctx context.Context, obj *versioning.Repository, name string) (*models.Commit, error) {
	return r.Resolver.RepositoryBranch().Commit(ctx, &models.RepositoryBranch{
		Repository: obj,
		Name:       name,
	})
}
func (r *repositoryResolver) CommitByReference(ctx context.Context, obj *versioning.Repository, ref schema.CommitReference) (*models.Commit, error) {
	if ref.Branch != nil {
		return r.Branch(ctx, obj, *ref.Branch)
	}
	if ref.Tag != nil {
		return r.Tag(ctx, obj, *ref.Tag)
	}
	if ref.Commit != nil {
		return r.Commit(ctx, obj, *ref.Commit)
	}
	return nil, errors.EmptyReference(ctx)
}
func (r *repositoryResolver) resolveCommitReference(ctx context.Context, obj *versioning.Repository, ref schema.CommitReference) (string, error) {
	if ref.Branch != nil {
		res, err := r.Branch(ctx, obj, *ref.Branch)
		if err != nil {
			return "", err
		}
		return res.Commit.GetCommitSha(), nil
	}
	if ref.Tag != nil {
		res, err := r.Tag(ctx, obj, *ref.Tag)
		if err != nil {
			return "", err
		}
		return res.Commit.GetCommitSha(), nil
	}
	if ref.Commit != nil {
		res, err := r.Commit(ctx, obj, *ref.Commit)
		if err != nil {
			return "", err
		}
		return res.Commit.GetCommitSha(), nil
	}
	return "", errors.EmptyReference(ctx)
}
func (r *repositoryResolver) Diff(ctx context.Context, obj *versioning.Repository, a schema.CommitReference, b schema.CommitReference) ([]string, error) {
	idA, err := r.resolveCommitReference(ctx, obj, a)
	if err != nil {
		r.Logger.Error("failed to resolve A", zap.Error(err))
		return nil, err
	}
	idB, err := r.resolveCommitReference(ctx, obj, b)
	if err != nil {
		r.Logger.Error("failed to resolve B", zap.Error(err))
		return nil, err
	}
	res, err := r.Connections.Versioning.ComputeRepositoryDiff(ctx, &versioning.ComputeRepositoryDiffRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
		CommitA: idA,
		CommitB: idB,
	})
	if err != nil {
		r.Logger.Error("failed to compute repository diff", zap.Error(err))
		return nil, err
	}
	result := make([]string, len(res.GetDiffs()))
	for i, diff := range res.GetDiffs() {
		buffer := &bytes.Buffer{}
		if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(buffer, diff); err != nil {
			r.Logger.Error("failed to serialize blob", zap.Error(err))
			return nil, err
		}
		result[i] = buffer.String()
	}
	return result, nil
}
func (r *repositoryResolver) Log(ctx context.Context, obj *versioning.Repository, commit schema.CommitReference) (*schema.Commits, error) {
	commitId, err := r.resolveCommitReference(ctx, obj, commit)
	if err != nil {
		r.Logger.Error("failed to resolve A", zap.Error(err))
		return nil, err
	}

	res, err := r.Connections.Versioning.ListCommitsLog(ctx, &versioning.ListCommitsLogRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
		CommitSha: commitId,
	})
	if err != nil {
		r.Logger.Error("failed to get repositories", zap.Error(err))
		return nil, err
	}

	commits := make([]*models.Commit, len(res.GetCommits()))
	for i, commit := range res.GetCommits() {
		commits[i] = &models.Commit{
			Repository: obj,
			Commit:     commit,
		}
	}

	return &schema.Commits{
		Commits: commits,
	}, nil
}
func (r *repositoryResolver) Delete(ctx context.Context, obj *versioning.Repository) (bool, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.DeleteOutsideMutation(ctx).Message)
		return false, errors.DeleteOutsideMutation(ctx)
	}
	res, err := r.Connections.Versioning.DeleteRepository(ctx, &versioning.DeleteRepositoryRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
	})
	if err != nil {
		r.Logger.Error("failed to delete repository", zap.Error(err))
		return false, err
	}

	return res.GetStatus(), nil
}
func (r *repositoryResolver) AddLabels(ctx context.Context, obj *versioning.Repository, labels []string) (*versioning.Repository, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	res, err := r.Connections.Metadata.AddLabels(ctx, &metadata.AddLabelsRequest{
		Id: &metadata.IdentificationType{
			IdType: metadata.IDTypeEnum_VERSIONING_REPOSITORY,
			Id:     &metadata.IdentificationType_IntId{IntId: obj.Id},
		},
		Labels: labels,
	})
	if err != nil {
		r.Logger.Error("failed to add labels", zap.Error(err))
		return nil, err
	}

	if !res.GetStatus() {
		r.Logger.Error(errors.ModelDbInternalError(ctx).Message)
		return nil, errors.ModelDbInternalError(ctx)
	}

	return r.Resolver.Query().Repository(ctx, r.id(obj))
}
func (r *repositoryResolver) DeleteLabels(ctx context.Context, obj *versioning.Repository, labels []string) (*versioning.Repository, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.DeleteOutsideMutation(ctx).Message)
		return nil, errors.DeleteOutsideMutation(ctx)
	}
	res, err := r.Connections.Metadata.DeleteLabels(ctx, &metadata.DeleteLabelsRequest{
		Id: &metadata.IdentificationType{
			IdType: metadata.IDTypeEnum_VERSIONING_REPOSITORY,
			Id:     &metadata.IdentificationType_IntId{IntId: obj.Id},
		},
		Labels: labels,
	})
	if err != nil {
		r.Logger.Error("failed to delete labels", zap.Error(err))
		return nil, err
	}

	if !res.GetStatus() {
		r.Logger.Error(errors.ModelDbInternalError(ctx).Message)
		return nil, errors.ModelDbInternalError(ctx)
	}

	return r.Resolver.Query().Repository(ctx, r.id(obj))
}
func (r *repositoryResolver) Merge(ctx context.Context, obj *versioning.Repository, a schema.CommitReference, b schema.CommitReference, message *string, isDryRun *bool) (*schema.MergeResult, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.MergeOutsideMutation(ctx).Message)
		return nil, errors.MergeOutsideMutation(ctx)
	}
	if isDryRun == nil {
		val := false
		isDryRun = &val
	}
	idA, err := r.resolveCommitReference(ctx, obj, a)
	if err != nil {
		r.Logger.Error("failed to resolve A", zap.Error(err))
		return nil, err
	}
	idB, err := r.resolveCommitReference(ctx, obj, b)
	if err != nil {
		r.Logger.Error("failed to resolve B", zap.Error(err))
		return nil, err
	}
	var content *versioning.Commit
	if message != nil {
		content = &versioning.Commit{
			Message: *message,
		}
	}
	res, err := r.Connections.Versioning.MergeRepositoryCommits(ctx, &versioning.MergeRepositoryCommitsRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
		CommitShaA: idA,
		CommitShaB: idB,
		Content:    content,
		IsDryRun:   *isDryRun,
	})
	if err != nil {
		r.Logger.Error("failed to merge commit", zap.Error(err))
		return nil, err
	}

	var resultCommit, baseCommit *models.Commit
	var conflicts []string
	if res.GetCommit() != nil {
		resultCommit = &models.Commit{
			Commit:     res.GetCommit(),
			Repository: obj,
		}
	}
	if res.GetCommonBase() != nil {
		baseCommit = &models.Commit{
			Commit:     res.GetCommonBase(),
			Repository: obj,
		}
	}
	if len(res.GetConflicts()) > 0 {
		conflicts = make([]string, len(res.GetConflicts()))
		for i, diff := range res.GetConflicts() {
			buffer := &bytes.Buffer{}
			if err := (&jsonpb.Marshaler{OrigName: true}).Marshal(buffer, diff); err != nil {
				r.Logger.Error("failed to serialize diff", zap.Error(err))
				return nil, err
			}
			conflicts[i] = buffer.String()
		}
	}

	return &schema.MergeResult{
		Commit:     resultCommit,
		CommonBase: baseCommit,
		Conflicts:  conflicts,
	}, nil
}

type repositoryBranchResolver struct{ *Resolver }

func (r *repositoryBranchResolver) Commit(ctx context.Context, obj *models.RepositoryBranch) (*models.Commit, error) {
	res, err := r.Connections.Versioning.GetBranch(ctx, &versioning.GetBranchRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Repository.GetId(),
		},
		Branch: obj.Name,
	})
	if err != nil {
		r.Logger.Error("failed to get branch", zap.Error(err))
		return nil, err
	}
	return &models.Commit{
		Repository: obj.Repository,
		Commit:     res.GetCommit(),
	}, nil
}

type repositoryTagResolver struct{ *Resolver }

func (r *repositoryTagResolver) Commit(ctx context.Context, obj *models.RepositoryTag) (*models.Commit, error) {
	res, err := r.Connections.Versioning.GetTag(ctx, &versioning.GetTagRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.Repository.GetId(),
		},
		Tag: obj.Name,
	})
	if err != nil {
		r.Logger.Error("failed to get tag", zap.Error(err))
		return nil, err
	}
	return &models.Commit{
		Repository: obj.Repository,
		Commit:     res.GetCommit(),
	}, nil
}
