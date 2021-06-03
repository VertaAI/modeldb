verta_url <- "https://dev.verta.ai"

headers_char_post <- c(
  "Grpc-Metadata-source" ="PythonClient",
  "Grpc-Metadata-email"=Sys.getenv("VERTA_EMAIL") ,
  "Grpc-Metadata-developer_key"= Sys.getenv("VERTA_DEV_KEY") ,
  "Grpc-Metadata-scheme"= "https",
  "Content-type"="application/json"
)

modeldb_base <- "/api/v1/modeldb"


{{- range $path, $path_details := .paths }}
{{- range $op, $op_details := $path_details }}
{{$op_details.operationId}} <- function(inp) {
    resp <- {{upper $op}}(
        url = str_c(verta_url,modeldb_base,"{{$path}}"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
{{- end }}
{{- end }}
