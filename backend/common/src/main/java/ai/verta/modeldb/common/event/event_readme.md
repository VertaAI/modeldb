# Event logging system
Documentation for event system

Event table have main four columns which is
- event_uuid : string : unique uuid string
- event_type : string : event type which is depends on resource_type and CRUD operation
- workspace_id : long : current login user workspace id
- event_metadata : string : this field contains the json string which has multiple fields like mention below

### event types


### metadata fields
- service: we have service enum at authz which is [https://github.com/VertaAI/modeldb/blob/bf34d3551db574325d27c9379479626f81fb6844/protos/protos/public/uac/RoleService.proto#L14][Service]
- resource_type: it is a entity name from [https://github.com/VertaAI/modeldb/blob/bf34d3551db574325d27c9379479626f81fb6844/protos/protos/public/common/CommonService.proto#L70][ModelDBServiceResourceTypes]
- logged_time: current logging timestamp
- 

[Service]: https://github.com/VertaAI/modeldb/blob/bf34d3551db574325d27c9379479626f81fb6844/protos/protos/public/uac/RoleService.proto#L14

[ModelDBServiceResourceTypes]: https://github.com/VertaAI/modeldb/blob/bf34d3551db574325d27c9379479626f81fb6844/protos/protos/public/common/CommonService.proto#L70