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

class BuildScanExternalResults(object):
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
        'safety_status': 'BuildScanSafetyStatus',
        'url': 'str'
    }

    attribute_map = {
        'safety_status': 'safety_status',
        'url': 'url'
    }

    def __init__(self, safety_status=None, url=None):  # noqa: E501
        """BuildScanExternalResults - a model defined in Swagger"""  # noqa: E501
        self._safety_status = None
        self._url = None
        self.discriminator = None
        if safety_status is not None:
            self.safety_status = safety_status
        if url is not None:
            self.url = url

    @property
    def safety_status(self):
        """Gets the safety_status of this BuildScanExternalResults.  # noqa: E501


        :return: The safety_status of this BuildScanExternalResults.  # noqa: E501
        :rtype: BuildScanSafetyStatus
        """
        return self._safety_status

    @safety_status.setter
    def safety_status(self, safety_status):
        """Sets the safety_status of this BuildScanExternalResults.


        :param safety_status: The safety_status of this BuildScanExternalResults.  # noqa: E501
        :type: BuildScanSafetyStatus
        """

        self._safety_status = safety_status

    @property
    def url(self):
        """Gets the url of this BuildScanExternalResults.  # noqa: E501


        :return: The url of this BuildScanExternalResults.  # noqa: E501
        :rtype: str
        """
        return self._url

    @url.setter
    def url(self, url):
        """Sets the url of this BuildScanExternalResults.


        :param url: The url of this BuildScanExternalResults.  # noqa: E501
        :type: str
        """

        self._url = url

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
        if issubclass(BuildScanExternalResults, dict):
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
        if not isinstance(other, BuildScanExternalResults):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """Returns true if both objects are not equal"""
        return not self == other
