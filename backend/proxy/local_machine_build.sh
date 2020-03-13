echo 'Start processing for go.mod & proxy.go'
go build .
echo 'Finish processing for go.mod & proxy.go'
echo 'Executing proxy.go'
go run proxy.go
