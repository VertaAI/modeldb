package models

import "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"

type Commit struct {
	Repository *versioning.Repository
	Commit     *versioning.Commit
}

type NamedCommitFolder struct {
	Commit   *Commit
	Location []string
	Name     string
}

type NamedCommitBlob struct {
	Commit   *Commit
	Location []string
	Name     string
}

type CommitBlob struct {
	Commit   *Commit
	Location []string
	Content  string
}

func (CommitBlob) IsCommitElement() {}
