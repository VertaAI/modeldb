1) Re-run "go run github.com/99designs/gqlgen" when you update the schema.
2) Move modifications from "resolve.go" to the different resolvers.
2a) You can use "go build ./..." to find which pieces are missing.
3) Remove resolve.go.
