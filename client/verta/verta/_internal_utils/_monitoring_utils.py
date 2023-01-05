# -*- coding: utf-8 -*-
from typing import Any, Dict, List


from verta._protos.public.uac import Collaborator_pb2 as _Collaborator
from verta._protos.public.common import CommonService_pb2 as _CommonService

def _body_from_args(
        args: Dict[str, Any],
        exclude_args: List[str]
        ) -> Dict:
    """
    Shortcut for processing the args from other class functions into a dict to be used as the body of a request.
    Requires that parameter names be the same as the expected proto variables in the request.

    Parameters
    ----------
    args: Dict[str, Any]
        Dict of functions args (locals()) to be packaged up as a dict.

    Returns
    -------
    Dict[str, Any]
    """
    body = dict()
    arg_keys = args.keys()
    if 'workspace' in arg_keys:
        if isinstance(args['workspace'], int):
            body['workspace_id'] = args['workspace']
        if isinstance(args['workspace'], str):
            body['workspace_name'] = args['workspace']
        del args['workspace']
    # Make sure we don't mistakenly include the classes "self" or the connection arg.
    for key in exclude_args:
        if key in arg_keys:
            del args[key]
    for k, v in args.items():
        if v:
            body[k] = v
    return body


def validate_resource_visibility(
        resource_visibility: str
        ) -> Dict[str, Any]:
    """
    Verify that provided value for resource_visibility is valid.
    Parameters
    ----------
    resource_visibility: str
        A string representing the desired visibility setting for a resource

    Returns
    -------
    Dict[str, str]

    """
    ok_rv_values = [x[0] for x in _Collaborator.ResourceVisibility.items()]
    if resource_visibility not in ok_rv_values:
        raise ValueError(f"value for \"resource_visibility\" must be one of {ok_rv_values}. "
                         f"Received \"{resource_visibility}\" instead.")
    return dict(resource_visibility=(_Collaborator.ResourceVisibility.Value(resource_visibility)))


def validate_custom_permission(
        custom_permission: str
        ) -> Dict[str, Any]:
    """
    Verify that provided value for custom_permission is valid.
    Parameters
    ----------
    custom_permission: str
        A string representing custom permissions.

    Returns
    -------
    Dict[str, str]

    """
    ok_cp_values = [x[0] for x in _CommonService.CollaboratorTypeEnum.CollaboratorType.items()]
    if custom_permission not in ok_cp_values:
        raise ValueError(f"value for \"custom_permission\" must be one of {ok_cp_values}. "
                         f"Received \"{custom_permission}\" instead.")
    return dict(custom_permission=(_Collaborator.CollaboratorPermissions(
            collaborator_type=_CommonService.CollaboratorTypeEnum.CollaboratorType.Value(custom_permission)
            )
        )
    )
