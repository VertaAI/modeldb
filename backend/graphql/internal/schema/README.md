# GraphQL schema

This folder contains all the information about the graphql schema exposed for ModelDB. It's mainly
used by the Web App for efficiency in loading and simplified querying.

## Generated resolvers files
A lot of the resolution for the schema is auto-generated. The generator will automatically add
resolvers for fields inside the structure it knows, and create methods to resolve the ones it doesn't
know.

To re-generate the files, you can:
1. Run `generate.sh`
2. Copy modifications from "resolve.go" to the different resolvers.
3. You can use "go build ./..." to find which pieces are missing.

## Contents and folder structure
The main contents of this file are:
1. `models_gen.go` contains all the data models and interfaces for the graphql interface;
2. `generated.go` contains the resolution logic;
3. `resolver.go` contains all resolvers. This file is not actually used and serves just as a reference
   to extract the interface resolvers must satisfy;
4. `gqlgen.yaml` is the configuration for the graphql generator, mainly specifying which data structs
   provide the models for different types;
5. `dataloader` contains partially auto-generated dataloaders, which aim to batch calls and avoid
   repeated resolution of the same entities multiple times in a single call;
6. `definition` contains the graphql schema. These files are glue together for `schema.graphql`;
7. `errors` provides a single package with all the errors returned to the user, so that we can easily
   identify and fix messages;
8. `models` contains all hand-defined models for graphql types, which is necessary when the schema
   doesn't contain all the information actually necessary to resolve a field or when you want some
   field to be resolved on demand via methods (the default for the generator is that all fields will
   be present in the object itself);
9. `pagination` has some tools to help deal with pagination, which is repeated in many queries;
10. `resolvers` contains all the resolvers that actually fetch information as necessary for graphql
    queries and mutations, according to the schema.
