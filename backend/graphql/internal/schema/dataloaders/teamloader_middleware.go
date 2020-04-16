package dataloaders

import (
	"context"
	"net/http"
	"sync"
	"time"

	"github.com/VertaAI/modeldb/backend/graphql/internal/server/connections"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"github.com/julienschmidt/httprouter"
)

type teamLoaderKeyType string

const teamLoaderKey teamLoaderKeyType = "teamloader"

func TeamDataloaderMiddleware(conn *connections.Connections) func(httprouter.Handle) httprouter.Handle {
	return func(next httprouter.Handle) httprouter.Handle {
		return func(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
			if conn.HasUac() {
				teamloaderConfig := TeamLoaderConfig{
					MaxBatch: 100,
					Wait:     1 * time.Millisecond,
					Fetch: func(reqs []string) ([]*ai_verta_uac.Team, []error) {
						errors := make([]error, len(reqs))

						teams := make([]*ai_verta_uac.Team, len(reqs))
						var waitgroup sync.WaitGroup
						waitgroup.Add(len(reqs))
						for i, req := range reqs {
							go func(i int, req string) {
								res, err := conn.Team.GetTeamById(
									r.Context(),
									&ai_verta_uac.GetTeamById{TeamId: req},
								)
								teams[i] = res.GetTeam()
								errors[i] = err
								waitgroup.Done()
							}(i, req)
						}
						waitgroup.Wait()

						return teams, errors
					},
				}

				ctx := context.WithValue(r.Context(), teamLoaderKey, NewTeamLoader(teamloaderConfig))
				r = r.WithContext(ctx)
			}
			next(w, r, ps)
		}
	}
}

func GetTeamById(ctx context.Context, id string) (*uac.Team, error) {
	teamLoader, teamLoaderOk := ctx.Value(teamLoaderKey).(*TeamLoader)
	if !teamLoaderOk {
		return &uac.Team{}, nil
	}

	return teamLoader.Load(id)
}
