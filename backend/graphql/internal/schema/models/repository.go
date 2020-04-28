package models

import "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"

type RepositoryTag struct {
	Repository *versioning.Repository
	Name       string
}

type RepositoryBranch struct {
	Repository *versioning.Repository
	Name       string
}
