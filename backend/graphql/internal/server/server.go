package server

import (
	"log"
	"net/http"
	"os"

	"github.com/99designs/gqlgen-contrib/gqlapollotracing"
	"github.com/99designs/gqlgen/handler"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/resolvers"
	"github.com/VertaAI/modeldb/backend/graphql/internal/server/connections"
	"github.com/julienschmidt/httprouter"
	"go.uber.org/zap"
)

func NewServer(logger *zap.Logger) *http.Server {
	conn, err := connections.NewConnections(logger)
	if err != nil {
		panic(err)
	}

	router := httprouter.New()
	router.RedirectTrailingSlash = false
	router.RedirectFixedPath = false

	queryHandler := handler.GraphQL(
		schema.NewExecutableSchema(schema.Config{Resolvers: &resolvers.Resolver{
			Logger:      logger,
			Connections: conn,
		}}),
		handler.RequestMiddleware(gqlapollotracing.RequestMiddleware()),
		handler.Tracer(gqlapollotracing.NewTracer()),
		handler.ErrorPresenter(errors.Presenter),
	)

	router.Handle(
		"POST",
		"/query",
		dataloaders.UserDataloaderMiddleware(conn)(
			dataloaders.TeamDataloaderMiddleware(conn)(
				func(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
					queryHandler.ServeHTTP(w, r)
				},
			),
		),
	)

	query := os.Getenv("QUERY_PATH")
	if query == "" {
		query = "/query"
	}

	router.Handle(
		"GET",
		"/playground",
		func(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
			handler.Playground("GraphQL", query)(w, r)
		},
	)

	port := os.Getenv("SERVER_HTTP_PORT")
	if port == "" {
		port = "4000"
	}
	log.Println("SERVER_HTTP_PORT : " + port)

	localServer := &http.Server{
		Handler: router,
		Addr:    ":" + port,
	}

	return localServer
}
