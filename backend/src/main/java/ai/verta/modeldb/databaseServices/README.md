# ai.verta.modeldb.documentServices Package

This package has one common interface (DocumentServices.java) for Document type databases and other file (MongoService.java) is a Mongo service implementation class.

MongoService.java services has implementation of DocumentService interface and this service implementation of MongoDB database so MongoDB syntax is used there. This is a common Mongo service for all other entity service (ProjectService, ExperimentService etc.).