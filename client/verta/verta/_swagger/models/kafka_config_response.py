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

class KafkaConfigResponse(object):
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
        'update_request': 'KafkaConfigUpdate',
        'status': 'KafkaStatusEnum',
        'message': 'str',
        'cluster_config': 'KafkaClusterConfig'
    }

    attribute_map = {
        'update_request': 'update_request',
        'status': 'status',
        'message': 'message',
        'cluster_config': 'cluster_config'
    }

    def __init__(self, update_request=None, status=None, message=None, cluster_config=None):  # noqa: E501
        """KafkaConfigResponse - a model defined in Swagger"""  # noqa: E501
        self._update_request = None
        self._status = None
        self._message = None
        self._cluster_config = None
        self.discriminator = None
        if update_request is not None:
            self.update_request = update_request
        if status is not None:
            self.status = status
        if message is not None:
            self.message = message
        if cluster_config is not None:
            self.cluster_config = cluster_config

    @property
    def update_request(self):
        """Gets the update_request of this KafkaConfigResponse.  # noqa: E501


        :return: The update_request of this KafkaConfigResponse.  # noqa: E501
        :rtype: KafkaConfigUpdate
        """
        return self._update_request

    @update_request.setter
    def update_request(self, update_request):
        """Sets the update_request of this KafkaConfigResponse.


        :param update_request: The update_request of this KafkaConfigResponse.  # noqa: E501
        :type: KafkaConfigUpdate
        """

        self._update_request = update_request

    @property
    def status(self):
        """Gets the status of this KafkaConfigResponse.  # noqa: E501


        :return: The status of this KafkaConfigResponse.  # noqa: E501
        :rtype: KafkaStatusEnum
        """
        return self._status

    @status.setter
    def status(self, status):
        """Sets the status of this KafkaConfigResponse.


        :param status: The status of this KafkaConfigResponse.  # noqa: E501
        :type: KafkaStatusEnum
        """

        self._status = status

    @property
    def message(self):
        """Gets the message of this KafkaConfigResponse.  # noqa: E501


        :return: The message of this KafkaConfigResponse.  # noqa: E501
        :rtype: str
        """
        return self._message

    @message.setter
    def message(self, message):
        """Sets the message of this KafkaConfigResponse.


        :param message: The message of this KafkaConfigResponse.  # noqa: E501
        :type: str
        """

        self._message = message

    @property
    def cluster_config(self):
        """Gets the cluster_config of this KafkaConfigResponse.  # noqa: E501


        :return: The cluster_config of this KafkaConfigResponse.  # noqa: E501
        :rtype: KafkaClusterConfig
        """
        return self._cluster_config

    @cluster_config.setter
    def cluster_config(self, cluster_config):
        """Sets the cluster_config of this KafkaConfigResponse.


        :param cluster_config: The cluster_config of this KafkaConfigResponse.  # noqa: E501
        :type: KafkaClusterConfig
        """

        self._cluster_config = cluster_config

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
        if issubclass(KafkaConfigResponse, dict):
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
        if not isinstance(other, KafkaConfigResponse):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """Returns true if both objects are not equal"""
        return not self == other
