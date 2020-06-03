package main

import (
	"github.com/VertaAI/modeldb/backend/graphql/internal/server"
	"go.uber.org/zap"
)

func main() {
	logger, _ := zap.NewDevelopment()

	server := server.NewServer(logger)
	if err := server.ListenAndServe(); err != nil {
		panic(err)
	}
}
