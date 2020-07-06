FROM golang:1.12.5-alpine3.9 as builder
RUN apk add --no-cache git mercurial bash

WORKDIR /tmp/build

ADD protos/gen/go ./protos/gen/go
ADD go.mod .
ADD go.sum .
COPY backend/graphql/ ./backend/graphql/
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /graphql ./backend/graphql/cmd/graphql.go

FROM scratch
COPY --from=builder /graphql /app/bin/graphql
EXPOSE 4000
ENTRYPOINT ["/app/bin/graphql"]
