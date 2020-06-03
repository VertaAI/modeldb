package errors

import (
	"fmt"

	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
)

var ModelDbInternalFailure = fmt.Errorf("Internal failure in ModelDB; please report")
var AddLabelsOutsideMutation = fmt.Errorf("Tried to add labels outside of a mutation")
var DeleteLabelsOutsideMutation = fmt.Errorf("Tried to delete labels outside of a mutation")
var DeleteOutsideMutation = fmt.Errorf("Tried to delete outside of a mutation")
var CreateOutsideMutation = fmt.Errorf("Tried to create outside of a mutation")
var MergeOutsideMutation = fmt.Errorf("Tried to merge outside of a mutation")
var SetTagOutsideMutation = fmt.Errorf("Tried to set tag outside of a mutation")
var SetBranchOutsideMutation = fmt.Errorf("Tried to set branch outside of a mutation")
var InvalidNextToken = fmt.Errorf("Invalid next token")
var NextOrQuery = fmt.Errorf("Only one of next or query must be present")
var FailedToFetchAuth = fmt.Errorf("Failed to fetch auth information")
var InvalidTypeFromModeldb = fmt.Errorf("Got invalid type from ModelDB; please report")
var EmptyReferenceToCommit = fmt.Errorf("Empty referene to commit")

func UnknownCollaboratorType(t common.EntitiesEnum_EntitiesTypes) error {
	return fmt.Errorf("Unknown entity type \"%s\"", common.EntitiesEnum_EntitiesTypes_name[int32(t)])
}

func UnknownTypeForValue(v common.ValueTypeEnum_ValueType) error {
	return fmt.Errorf("Unknown type for value \"%s\"", v.String())
}

func AtPosition(e error, i int) error {
	return fmt.Errorf("%s at position %d", e.Error(), i+1)
}
