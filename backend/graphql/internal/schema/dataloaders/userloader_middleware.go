package dataloaders

import (
	"context"
	"net/http"
	"sync"
	"time"

	"github.com/VertaAI/modeldb/backend/graphql/internal/server/connections"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"github.com/julienschmidt/httprouter"
)

type userLoaderKeyType string

const userLoaderKey userLoaderKeyType = "userloader"

func UserDataloaderMiddleware(conn *connections.Connections) func(httprouter.Handle) httprouter.Handle {
	return func(next httprouter.Handle) httprouter.Handle {
		return func(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
			if conn.HasUac() {
				userloaderConfig := UserLoaderConfig{
					MaxBatch: 100,
					Wait:     1 * time.Millisecond,
					Fetch: func(reqs []string) ([]*ai_verta_uac.UserInfo, []error) {
						errors := make([]error, len(reqs))

						users := make([]*ai_verta_uac.UserInfo, len(reqs))
						var waitgroup sync.WaitGroup
						waitgroup.Add(len(reqs))
						for i, req := range reqs {
							go func(i int, req string) {
								users[i], errors[i] = conn.UAC.GetUser(
									r.Context(),
									&ai_verta_uac.GetUser{UserId: req},
								)
								waitgroup.Done()
							}(i, req)
						}
						waitgroup.Wait()

						return users, errors
					},
				}

				ctx := context.WithValue(r.Context(), userLoaderKey, NewUserLoader(userloaderConfig))
				r = r.WithContext(ctx)
			}
			next(w, r, ps)
		}
	}
}

func GetUserById(ctx context.Context, id string) (*ai_verta_uac.UserInfo, error) {
	userLoader, userLoaderOk := ctx.Value(userLoaderKey).(*UserLoader)
	if !userLoaderOk {
		return &ai_verta_uac.UserInfo{
			FullName: "Unknwon User",
			Email:    "unknown@user.com",
			VertaInfo: &ai_verta_uac.VertaUserInfo{
				UserId:   "",
				Username: "UnkwnownUser",
			},
		}, nil
	}

	return userLoader.Load(id)
}
