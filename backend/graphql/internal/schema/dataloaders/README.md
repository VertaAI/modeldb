1) Get the generator `go get github.com/vektah/dataloaden`
2) Run the generation
   like `dataloaden UserLoader string *github.com/VertaAI/modeldb-go/protos/private/uac.UserInfo` in
   the **same directory** as `models_gen.go`
3) Move the generated files to `dataloaders` and adjust any imports
4) Add a constructor method as described in https://github.com/vektah/dataloaden
5) Use as described in https://gqlgen.com/reference/dataloaders/
