#!/bin/bash

go run r-gen.go -input gen/swagger/protos/public/modeldb/ProjectService.swagger.json -output project.r -template r-template.tmpl.r
