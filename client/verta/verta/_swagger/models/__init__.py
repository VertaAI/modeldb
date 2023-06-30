# coding: utf-8

# flake8: noqa
"""
    Deployment API

    No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)  # noqa: E501

    OpenAPI spec version: 1.0.0
    
    Generated by: https://github.com/swagger-api/swagger-codegen.git
"""

from __future__ import absolute_import

# import models into model package
from swagger_client.models.autoscaling_metric import AutoscalingMetric
from swagger_client.models.autoscaling_metric_parameter_type import AutoscalingMetricParameterType
from swagger_client.models.autoscaling_metric_parameters import AutoscalingMetricParameters
from swagger_client.models.autoscaling_request import AutoscalingRequest
from swagger_client.models.autoscaling_request_metrics import AutoscalingRequestMetrics
from swagger_client.models.autoscaling_request_parameters import AutoscalingRequestParameters
from swagger_client.models.autoscaling_request_quantities import AutoscalingRequestQuantities
from swagger_client.models.build_create import BuildCreate
from swagger_client.models.build_deploy_policy import BuildDeployPolicy
from swagger_client.models.build_response import BuildResponse
from swagger_client.models.build_scan_create import BuildScanCreate
from swagger_client.models.build_scan_detail import BuildScanDetail
from swagger_client.models.build_scan_response import BuildScanResponse
from swagger_client.models.build_scan_safety_status import BuildScanSafetyStatus
from swagger_client.models.build_scan_severity import BuildScanSeverity
from swagger_client.models.build_scan_status import BuildScanStatus
from swagger_client.models.build_scan_update import BuildScanUpdate
from swagger_client.models.build_status import BuildStatus
from swagger_client.models.build_update import BuildUpdate
from swagger_client.models.build_update_artifacts import BuildUpdateArtifacts
from swagger_client.models.builds_response import BuildsResponse
from swagger_client.models.canary_rollout import CanaryRollout
from swagger_client.models.canary_rollout_rollout import CanaryRolloutRollout
from swagger_client.models.canary_rule import CanaryRule
from swagger_client.models.canary_rule_category import CanaryRuleCategory
from swagger_client.models.canary_rule_parameter_type import CanaryRuleParameterType
from swagger_client.models.canary_rule_rule_parameters import CanaryRuleRuleParameters
from swagger_client.models.canary_strategy import CanaryStrategy
from swagger_client.models.canary_strategy_rule_parameters import CanaryStrategyRuleParameters
from swagger_client.models.canary_strategy_rules import CanaryStrategyRules
from swagger_client.models.endpoint_create import EndpointCreate
from swagger_client.models.endpoint_create_custom_permission import EndpointCreateCustomPermission
from swagger_client.models.endpoint_patch import EndpointPatch
from swagger_client.models.endpoint_response import EndpointResponse
from swagger_client.models.endpoint_search import EndpointSearch
from swagger_client.models.environment_variable_source_enum import EnvironmentVariableSourceEnum
from swagger_client.models.inline_response200 import InlineResponse200
from swagger_client.models.inline_response2001 import InlineResponse2001
from swagger_client.models.inline_response2002 import InlineResponse2002
from swagger_client.models.inline_response2003 import InlineResponse2003
from swagger_client.models.inline_response2004 import InlineResponse2004
from swagger_client.models.inline_response422 import InlineResponse422
from swagger_client.models.kafka_config_response import KafkaConfigResponse
from swagger_client.models.kafka_config_update import KafkaConfigUpdate
from swagger_client.models.kafka_status_enum import KafkaStatusEnum
from swagger_client.models.list_deployments_response import ListDeploymentsResponse
from swagger_client.models.list_deployments_response_inner import ListDeploymentsResponseInner
from swagger_client.models.operations_manifest import OperationsManifest
from swagger_client.models.operations_manifest_image import OperationsManifestImage
from swagger_client.models.stage_build_metrics import StageBuildMetrics
from swagger_client.models.stage_build_metrics_latency import StageBuildMetricsLatency
from swagger_client.models.stage_component_status_enum import StageComponentStatusEnum
from swagger_client.models.stage_create import StageCreate
from swagger_client.models.stage_log_item import StageLogItem
from swagger_client.models.stage_logs import StageLogs
from swagger_client.models.stage_metrics import StageMetrics
from swagger_client.models.stage_metrics_inner import StageMetricsInner
from swagger_client.models.stage_patch import StagePatch
from swagger_client.models.stage_response import StageResponse
from swagger_client.models.stage_response_components import StageResponseComponents
from swagger_client.models.stage_status import StageStatus
from swagger_client.models.stage_status_enum import StageStatusEnum
from swagger_client.models.stage_status_traffic_shape import StageStatusTrafficShape
from swagger_client.models.stage_status_traffic_shape_ratio import StageStatusTrafficShapeRatio
from swagger_client.models.stage_token_create import StageTokenCreate
from swagger_client.models.stage_token_response import StageTokenResponse
from swagger_client.models.stage_tokens_response import StageTokensResponse
from swagger_client.models.stage_update_request import StageUpdateRequest
from swagger_client.models.stage_update_request_env import StageUpdateRequestEnv
from swagger_client.models.stage_update_request_resources import StageUpdateRequestResources
from swagger_client.models.validate_docker_registry import ValidateDockerRegistry
