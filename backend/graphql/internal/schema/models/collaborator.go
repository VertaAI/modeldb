package models

import "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"

type UserCollaborator struct {
	*uac.GetCollaboratorResponse
}
type TeamCollaborator struct {
	*uac.GetCollaboratorResponse
}

func (UserCollaborator) IsCollaborator() {}
func (TeamCollaborator) IsCollaborator() {}
