# Event logging system
Documentation for event system

## Event main fields
Event object have main four fields like:
- `event_uuid`
- `event_type`
- `workspace_id`
- `event_metadata`

### Event UUID (event_uuid)
`datatype`: String

`description`: it will be auto generated UUID from individual backend services while logging events

### Workspace ID (workspace_id)
`datatype`: Long

`description`: it will be current login user workspace id

### Event Types (event_type)
`datatype`: String

`description`: event type which is depends on resource_type and CRUD operation

| Resource Name | Event Types |
| ------ | ------ |
| Project | - add.resource.project.add_project_succeeded <br/> - update.resource.project.update_project_succeeded <br/> - delete.resource.project.delete_project_succeeded |
| Experiment | - add.resource.experiment.add_experiment_succeeded <br/> - update.resource.experiment.update_experiment_succeeded <br/> - delete.resource.experiment.delete_experiment_succeeded |
| Experiment Run | - add.resource.experiment_run.add_experiment_run_succeeded <br/> - update.resource.experiment_run.update_experiment_run_succeeded <br/> - delete.resource.experiment_run.delete_experiment_run_succeeded |


### Event Metadata (event_metadata)
`datatype`: String

`description`: this field contains the json string which has multiple fields like mention below

- `(Required) service`: we have service enum at authz which is [Service](https://github.com/VertaAI/modeldb/blob/bf34d3551db574325d27c9379479626f81fb6844/protos/protos/public/uac/RoleService.proto#L14)
- `(Required) resource_type`: it is a entity name from [ModelDBServiceResourceTypes](https://github.com/VertaAI/modeldb/blob/bf34d3551db574325d27c9379479626f81fb6844/protos/protos/public/common/CommonService.proto#L70)
- `(Required) logged_time`: current logging timestamp
- `(Required) message`: event message about what happen on that api call
- `(Required) entity_id`: resource entity id for ex. project_id, repository_id, registered_model_id etc.
- `(Optional) updated_field`: if it is a separated field update call then it would be like metrics, attributes, artifacts, name etc. and if it will be creation or deletion call then it would be an empty
- `(Optional) updated_field_value`: if there will be a updated_field then this field will have json object with the relevant key and values like
  
  | updated_field | updated_field_value |
  | ------ | ------ |
  | attributes | {"attribute_keys":["key1","key2"]} <br> OR <br> {"attributes_delete_all":true} |
  | metrics | {"metric_keys":["key1","key2"]} <br> OR <br> {"metrics_delete_all":true} |
  | artifacts | {"artifact_keys":["key1","key2"]} <br> OR <br> {"artifacts_delete_all":true} |
  | .<br>.<br>. | .<br>.<br>. |
