package resolvers

import (
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/server/connections"
	"go.uber.org/zap"
)

type Resolver struct {
	Logger      *zap.Logger
	Connections *connections.Connections
}

var _ schema.ResolverRoot = &Resolver{}

func (r *Resolver) Artifact() schema.ArtifactResolver {
	return &artifactResolver{r}
}

func (r *Resolver) Commit() schema.CommitResolver {
	return &commitResolver{r}
}
func (r *Resolver) CommitBlob() schema.CommitBlobResolver {
	return &commitBlobResolver{r}
}
func (r *Resolver) Dataset() schema.DatasetResolver {
	return &datasetResolver{r}
}
func (r *Resolver) DatasetVersion() schema.DatasetVersionResolver {
	return &datasetVersionResolver{r}
}

func (r *Resolver) Experiment() schema.ExperimentResolver {
	return &experimentResolver{r}
}
func (r *Resolver) ExperimentRun() schema.ExperimentRunResolver {
	return &experimentRunResolver{r}
}
func (r *Resolver) Mutation() schema.MutationResolver {
	return &mutationResolver{r}
}
func (r *Resolver) NamedCommitBlob() schema.NamedCommitBlobResolver {
	return &namedCommitBlobResolver{r}
}
func (r *Resolver) NamedCommitFolder() schema.NamedCommitFolderResolver {
	return &namedCommitFolderResolver{r}
}
func (r *Resolver) Observation() schema.ObservationResolver {
	return &observationResolver{r}
}
func (r *Resolver) Organization() schema.OrganizationResolver {
	return &organizationResolver{r}
}
func (r *Resolver) Project() schema.ProjectResolver {
	return &projectResolver{r}
}
func (r *Resolver) Query() schema.QueryResolver {
	return &queryResolver{r}
}

func (r *Resolver) Repository() schema.RepositoryResolver {
	return &repositoryResolver{r}
}
func (r *Resolver) RepositoryBranch() schema.RepositoryBranchResolver {
	return &repositoryBranchResolver{r}
}
func (r *Resolver) RepositoryTag() schema.RepositoryTagResolver {
	return &repositoryTagResolver{r}
}
func (r *Resolver) Team() schema.TeamResolver {
	return &teamResolver{r}
}
func (r *Resolver) TeamCollaborator() schema.TeamCollaboratorResolver {
	return &teamCollaboratorResolver{r}
}
func (r *Resolver) User() schema.UserResolver {
	return &userResolver{r}
}
func (r *Resolver) UserCollaborator() schema.UserCollaboratorResolver {
	return &userCollaboratorResolver{r}
}
func (r *Resolver) Workspace() schema.WorkspaceResolver {
	return &workspaceResolver{r}
}
