verta_url <- "https://dev.verta.ai"

headers_char_post <- c(
  "Grpc-Metadata-source" ="PythonClient",
  "Grpc-Metadata-email"=Sys.getenv("VERTA_EMAIL") ,
  "Grpc-Metadata-developer_key"= Sys.getenv("VERTA_DEV_KEY") ,
  "Grpc-Metadata-scheme"= "https",
  "Content-type"="application/json"
)

modeldb_base <- "/api/v1/modeldb"
ProjectService_addProjectAttributes <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/addProjectAttributes"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_addProjectTag <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/addProjectTag"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_addProjectTags <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/addProjectTags"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_createProject <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/createProject"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deepCopyProject <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/deepCopyProject"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deleteArtifact <- function(inp) {
    resp <- DELETE(
        url = str_c(verta_url,modeldb_base,"/project/deleteArtifact"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deleteProject <- function(inp) {
    resp <- DELETE(
        url = str_c(verta_url,modeldb_base,"/project/deleteProject"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deleteProjectAttributes <- function(inp) {
    resp <- DELETE(
        url = str_c(verta_url,modeldb_base,"/project/deleteProjectAttributes"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deleteProjectTag <- function(inp) {
    resp <- DELETE(
        url = str_c(verta_url,modeldb_base,"/project/deleteProjectTag"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deleteProjectTags <- function(inp) {
    resp <- DELETE(
        url = str_c(verta_url,modeldb_base,"/project/deleteProjectTags"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_deleteProjects <- function(inp) {
    resp <- DELETE(
        url = str_c(verta_url,modeldb_base,"/project/deleteProjects"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_findProjects <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/findProjects"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getArtifacts <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getArtifacts"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectAttributes <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectAttributes"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectById <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectById"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectByName <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectByName"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectCodeVersion <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectCodeVersion"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectDatasetCount <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectDatasetCount"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectReadme <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectReadme"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectShortName <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectShortName"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjectTags <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjectTags"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getProjects <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getProjects"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getSummary <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/getSummary"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_getUrlForArtifact <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/getUrlForArtifact"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_logArtifacts <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/logArtifacts"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_logProjectCodeVersion <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/logProjectCodeVersion"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_setProjectReadme <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/setProjectReadme"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_setProjectShortName <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/setProjectShortName"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_updateProjectAttributes <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/updateProjectAttributes"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_updateProjectDescription <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/updateProjectDescription"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_updateProjectName <- function(inp) {
    resp <- POST(
        url = str_c(verta_url,modeldb_base,"/project/updateProjectName"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
ProjectService_verifyConnection <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,"/project/verifyConnection"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
on <- function(inp) {
    resp <- GET(
        url = str_c(verta_url,modeldb_base,/project/verifyConnection,sep = "/"),
        add_headers(
            .headers=headers_char_post
        ),
        body=inp,
        encode="json"
    )
    return(resp)
}
