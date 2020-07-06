FROM golang:1.12.5-alpine3.9 as builder
RUN apk add --no-cache git mercurial bash

WORKDIR /tmp/build

ADD protos/gen/go ./protos/gen/go
ADD go.mod .
ADD go.sum .
COPY backend/proxy/proxy.go ./backend/proxy/proxy.go
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /proxy ./backend/proxy

FROM scratch
COPY --from=builder /proxy /app/bin/proxy
EXPOSE 8080
ENTRYPOINT ["/app/bin/proxy"]
