package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

	mdb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	metadata "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/metadata"
	versioning "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	"github.com/grpc-ecosystem/grpc-gateway/runtime"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
)

func main() {
	address := os.Getenv("MDB_ADDRESS")
	if address == "" {
		address = "localhost:8085"
	}
	log.Println("MDB_ADDRESS : " + address)
	port := os.Getenv("SERVER_HTTP_PORT")
	if port == "" {
		port = "8080"
	}
	log.Println("SERVER_HTTP_PORT : " + port)
	mux := runtime.NewServeMux()
	opts := []grpc.DialOption{grpc.WithInsecure()}
	endpoints := []func(context.Context, *runtime.ServeMux, string, []grpc.DialOption) (err error){
		mdb.RegisterProjectServiceHandlerFromEndpoint,
		mdb.RegisterExperimentServiceHandlerFromEndpoint,
		mdb.RegisterExperimentRunServiceHandlerFromEndpoint,
		mdb.RegisterCommentServiceHandlerFromEndpoint,
		mdb.RegisterHydratedServiceHandlerFromEndpoint,
		mdb.RegisterDatasetServiceHandlerFromEndpoint,
		mdb.RegisterDatasetVersionServiceHandlerFromEndpoint,
		mdb.RegisterLineageServiceHandlerFromEndpoint,
		versioning.RegisterVersioningServiceHandlerFromEndpoint,
		metadata.RegisterMetadataServiceHandlerFromEndpoint,
	}
	for i, endpoint := range endpoints {
		if err := endpoint(context.Background(), mux, address, opts); err != nil {
			panic(fmt.Sprintf("failed to register endpoint %d", i))
		}
	}
	localServer := &http.Server{
		Handler: mux,
		Addr:    ":" + port,
	}
	log.Println("Starting verta-backend proxy on port : " + port)
	err := localServer.ListenAndServe()
	if err != http.ErrServerClosed {
		panic(err)
	}
}
