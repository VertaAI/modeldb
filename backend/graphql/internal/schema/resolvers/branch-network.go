package resolvers

import (
	"context"
	"sort"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
)

type NetworkCommit struct {
	SHA     string
	Parents []string
	Date    uint64
	Commit  *versioning.Commit
}

func (r *repositoryResolver) Network(ctx context.Context, obj *versioning.Repository) (*schema.BranchesNetwork, error) {
	resCommits, err := r.Connections.Versioning.ListCommits(ctx, &versioning.ListCommitsRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
	})
	if err != nil {
		panic(err)
	}

	mdbCommits := resCommits.GetCommits()
	commits := make([]NetworkCommit, len(mdbCommits))
	for i, c := range mdbCommits {
		commits[i] = NetworkCommit{
			SHA:     c.GetCommitSha(),
			Parents: c.GetParentShas(),
			Date:    c.GetDateCreated(),
			Commit:  c,
		}
	}

	sort.Slice(commits, func(i, j int) bool {
		return commits[i].Date < commits[j].Date
	})

	commitToIndex := make(map[string]int)
	for i, c := range commits {
		commitToIndex[c.SHA] = i
	}

	resBranches, err := r.Connections.Versioning.ListBranches(ctx, &versioning.ListBranchesRequest{
		RepositoryId: &versioning.RepositoryIdentification{
			RepoId: obj.GetId(),
		},
	})
	if err != nil {
		panic(err)
	}

	branches := resBranches.GetBranches()
	branchToCommit := make(map[string]string)
	for _, branch := range branches {
		resCommit, err := r.Connections.Versioning.GetBranch(ctx, &versioning.GetBranchRequest{
			RepositoryId: &versioning.RepositoryIdentification{
				RepoId: obj.GetId(),
			},
			Branch: branch,
		})
		if err != nil {
			panic(err)
		}

		branchToCommit[branch] = resCommit.GetCommit().GetCommitSha()
	}

	commitToColorExtended := make(map[string]int)
	// commitToColorCounter := int(0)

	masterCommit, masterOk := branchToCommit["master"]
	if !masterOk {
		panic("no master branch")
	}

	commitToColorExtended[masterCommit] = 0
	// // commitToColorCounter++

	for i, commit := range commits {
		if _, ok := commitToColorExtended[commit.SHA]; !ok {
			commitToColorExtended[commit.SHA] = i + 1
		}
	}

	for i := len(commits) - 1; i >= 0; i-- {
		commit := commits[i]
		commitColor := commitToColorExtended[commit.SHA]
		if len(commit.Parents) > 1 {
			for j, parent := range commit.Parents {
				if j != 0 && commitToColorExtended[parent] != 0 {
					// fmt.Printf("at %s, converting %s from %d to %d\n", commit.SHA, parent, commitToColorExtended[parent], commitColor)
					commitToColorExtended[parent] = commitColor
				}
			}
		} else if len(commit.Parents) == 1 {
			if commitToColorExtended[commit.Parents[0]] != 0 {
				commitToColorExtended[commit.Parents[0]] = commitColor
			}
		}
	}

	colorCompression := map[int]int{0: 0}
	commitToColorCounter := int(1)
	commitToColor := make(map[string]int)
	for _, commit := range commits {
		color := commitToColorExtended[commit.SHA]

		if _, ok := colorCompression[color]; !ok {
			colorCompression[color] = commitToColorCounter
			commitToColorCounter++
		}
		newColor := colorCompression[color]
		commitToColor[commit.SHA] = newColor
	}

	edges := make([]*schema.NetworkEdgeColor, 0)
	for _, commit := range commits {
		commitColor := commitToColor[commit.SHA]
		for _, parent := range commit.Parents {
			parentColor := commitToColor[parent]
			newEdge := &schema.NetworkEdgeColor{
				FromCommitIndex: commitToIndex[parent],
				ToCommitIndex:   commitToIndex[commit.SHA],
				Color:           parentColor,
				EdgeType:        schema.NetworkEdgeTypeDefault,
			}

			if parentColor != commitColor {
				if len(commit.Parents) > 1 {
					newEdge.EdgeType = schema.NetworkEdgeTypeMerge
					newEdge.Color = parentColor
				} else {
					newEdge.EdgeType = schema.NetworkEdgeTypeBranch
					newEdge.Color = commitColor
				}
			}

			edges = append(edges, newEdge)
		}
	}

	commitColorList := make([]*models.NetworkCommitColor, len(commits))
	for i, commit := range commits {
		commitColorList[i] = &models.NetworkCommitColor{
			Commit: &models.Commit{
				Commit:     commit.Commit,
				Repository: obj,
			},
			Color: commitToColor[commit.SHA],
		}
	}

	branchColorList := make([]*schema.NetworkBranchColor, len(branches))
	for i, branch := range branches {
		branchColorList[i] = &schema.NetworkBranchColor{
			Branch:      branch,
			Color:       commitToColor[branchToCommit[branch]],
			CommitIndex: commitToIndex[branchToCommit[branch]],
		}
	}

	return &schema.BranchesNetwork{
		Commits:  commitColorList,
		Branches: branchColorList,
		Edges:    edges,
	}, nil
}
