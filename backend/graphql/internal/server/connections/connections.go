package connections

import (
	"os"

	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/metadata"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type Connections struct {
	mdb *grpc.ClientConn
	uac *grpc.ClientConn

	Experiment     ai_verta_modeldb.ExperimentServiceClient
	ExperimentRun  ai_verta_modeldb.ExperimentRunServiceClient
	Project        ai_verta_modeldb.ProjectServiceClient
	Dataset        ai_verta_modeldb.DatasetServiceClient
	DatasetVersion ai_verta_modeldb.DatasetVersionServiceClient

	Versioning versioning.VersioningServiceClient

	Metadata metadata.MetadataServiceClient

	Collaborator  ai_verta_uac.CollaboratorServiceClient
	Organization  ai_verta_uac.OrganizationServiceClient
	Team          ai_verta_uac.TeamServiceClient
	UAC           ai_verta_uac.UACServiceClient
	Authorization ai_verta_uac.AuthzServiceClient
}

func (c *Connections) HasUac() bool {
	return c.uac != nil
}

func NewConnections(logger *zap.Logger) (*Connections, error) {
	mdbAddress := os.Getenv("MDB_ADDRESS")
	if mdbAddress == "" {
		mdbAddress = "localhost:8085"
	}
	uacAddress := os.Getenv("UAC_ADDRESS")

	var UAC *grpc.ClientConn

	MDB, err := grpc.Dial(mdbAddress, grpc.WithInsecure())
	if err != nil {
		logger.Error("failed to connect to MDB", zap.Error(err))
		return nil, err
	}

	if uacAddress != "" {
		UAC, err = grpc.Dial(uacAddress, grpc.WithInsecure())
		if err != nil {
			logger.Error("failed to connect to UAC", zap.Error(err))
			return nil, err
		}
	}

	c := &Connections{
		mdb: MDB,
		uac: UAC,
	}

	c.Project = ai_verta_modeldb.NewProjectServiceClient(c.mdb)
	c.Experiment = ai_verta_modeldb.NewExperimentServiceClient(c.mdb)
	c.ExperimentRun = ai_verta_modeldb.NewExperimentRunServiceClient(c.mdb)
	c.Dataset = ai_verta_modeldb.NewDatasetServiceClient(c.mdb)
	c.DatasetVersion = ai_verta_modeldb.NewDatasetVersionServiceClient(c.mdb)

	c.Versioning = versioning.NewVersioningServiceClient(c.mdb)

	c.Metadata = metadata.NewMetadataServiceClient(c.mdb)

	c.Collaborator = ai_verta_uac.NewCollaboratorServiceClient(c.uac)
	c.Organization = ai_verta_uac.NewOrganizationServiceClient(c.uac)
	c.Team = ai_verta_uac.NewTeamServiceClient(c.uac)
	c.UAC = ai_verta_uac.NewUACServiceClient(c.uac)
	c.Authorization = ai_verta_uac.NewAuthzServiceClient(c.uac)

	return c, nil
}
