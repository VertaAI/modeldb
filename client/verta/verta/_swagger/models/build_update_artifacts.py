# coding: utf-8

"""
    Deployment API

    No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)  # noqa: E501

    OpenAPI spec version: 1.0.0
    
    Generated by: https://github.com/swagger-api/swagger-codegen.git
"""

import pprint
import re  # noqa: F401

import six

class BuildUpdateArtifacts(object):
    """NOTE: This class is auto generated by the swagger code generator program.

    Do not edit the class manually.
    """
    """
    Attributes:
      swagger_types (dict): The key is attribute name
                            and the value is attribute type.
      attribute_map (dict): The key is attribute name
                            and the value is json key in definition.
    """
    swagger_types = {
        'run_id': 'str',
        'model_version_id': 'int',
        'base_image': 'str',
        'self_contained': 'bool',
        'delete_all_base_artifacts': 'bool'
    }

    attribute_map = {
        'run_id': 'run_id',
        'model_version_id': 'model_version_id',
        'base_image': 'base_image',
        'self_contained': 'self_contained',
        'delete_all_base_artifacts': 'delete_all_base_artifacts'
    }

    def __init__(self, run_id=None, model_version_id=None, base_image=None, self_contained=None, delete_all_base_artifacts=None):  # noqa: E501
        """BuildUpdateArtifacts - a model defined in Swagger"""  # noqa: E501
        self._run_id = None
        self._model_version_id = None
        self._base_image = None
        self._self_contained = None
        self._delete_all_base_artifacts = None
        self.discriminator = None
        if run_id is not None:
            self.run_id = run_id
        if model_version_id is not None:
            self.model_version_id = model_version_id
        self.base_image = base_image
        if self_contained is not None:
            self.self_contained = self_contained
        if delete_all_base_artifacts is not None:
            self.delete_all_base_artifacts = delete_all_base_artifacts

    @property
    def run_id(self):
        """Gets the run_id of this BuildUpdateArtifacts.  # noqa: E501


        :return: The run_id of this BuildUpdateArtifacts.  # noqa: E501
        :rtype: str
        """
        return self._run_id

    @run_id.setter
    def run_id(self, run_id):
        """Sets the run_id of this BuildUpdateArtifacts.


        :param run_id: The run_id of this BuildUpdateArtifacts.  # noqa: E501
        :type: str
        """

        self._run_id = run_id

    @property
    def model_version_id(self):
        """Gets the model_version_id of this BuildUpdateArtifacts.  # noqa: E501


        :return: The model_version_id of this BuildUpdateArtifacts.  # noqa: E501
        :rtype: int
        """
        return self._model_version_id

    @model_version_id.setter
    def model_version_id(self, model_version_id):
        """Sets the model_version_id of this BuildUpdateArtifacts.


        :param model_version_id: The model_version_id of this BuildUpdateArtifacts.  # noqa: E501
        :type: int
        """

        self._model_version_id = model_version_id

    @property
    def base_image(self):
        """Gets the base_image of this BuildUpdateArtifacts.  # noqa: E501


        :return: The base_image of this BuildUpdateArtifacts.  # noqa: E501
        :rtype: str
        """
        return self._base_image

    @base_image.setter
    def base_image(self, base_image):
        """Sets the base_image of this BuildUpdateArtifacts.


        :param base_image: The base_image of this BuildUpdateArtifacts.  # noqa: E501
        :type: str
        """
        if base_image is None:
            raise ValueError("Invalid value for `base_image`, must not be `None`")  # noqa: E501

        self._base_image = base_image

    @property
    def self_contained(self):
        """Gets the self_contained of this BuildUpdateArtifacts.  # noqa: E501


        :return: The self_contained of this BuildUpdateArtifacts.  # noqa: E501
        :rtype: bool
        """
        return self._self_contained

    @self_contained.setter
    def self_contained(self, self_contained):
        """Sets the self_contained of this BuildUpdateArtifacts.


        :param self_contained: The self_contained of this BuildUpdateArtifacts.  # noqa: E501
        :type: bool
        """

        self._self_contained = self_contained

    @property
    def delete_all_base_artifacts(self):
        """Gets the delete_all_base_artifacts of this BuildUpdateArtifacts.  # noqa: E501


        :return: The delete_all_base_artifacts of this BuildUpdateArtifacts.  # noqa: E501
        :rtype: bool
        """
        return self._delete_all_base_artifacts

    @delete_all_base_artifacts.setter
    def delete_all_base_artifacts(self, delete_all_base_artifacts):
        """Sets the delete_all_base_artifacts of this BuildUpdateArtifacts.


        :param delete_all_base_artifacts: The delete_all_base_artifacts of this BuildUpdateArtifacts.  # noqa: E501
        :type: bool
        """

        self._delete_all_base_artifacts = delete_all_base_artifacts

    def to_dict(self):
        """Returns the model properties as a dict"""
        result = {}

        for attr, _ in six.iteritems(self.swagger_types):
            value = getattr(self, attr)
            if isinstance(value, list):
                result[attr] = list(map(
                    lambda x: x.to_dict() if hasattr(x, "to_dict") else x,
                    value
                ))
            elif hasattr(value, "to_dict"):
                result[attr] = value.to_dict()
            elif isinstance(value, dict):
                result[attr] = dict(map(
                    lambda item: (item[0], item[1].to_dict())
                    if hasattr(item[1], "to_dict") else item,
                    value.items()
                ))
            else:
                result[attr] = value
        if issubclass(BuildUpdateArtifacts, dict):
            for key, value in self.items():
                result[key] = value

        return result

    def to_str(self):
        """Returns the string representation of the model"""
        return pprint.pformat(self.to_dict())

    def __repr__(self):
        """For `print` and `pprint`"""
        return self.to_str()

    def __eq__(self, other):
        """Returns true if both objects are equal"""
        if not isinstance(other, BuildUpdateArtifacts):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """Returns true if both objects are not equal"""
        return not self == other
