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

class CanaryRule(object):
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
        'rule_id': 'int',
        'name': 'str',
        'description': 'str',
        'category': 'CanaryRuleCategory',
        'rule_parameters': 'list[CanaryRuleRuleParameters]'
    }

    attribute_map = {
        'rule_id': 'rule_id',
        'name': 'name',
        'description': 'description',
        'category': 'category',
        'rule_parameters': 'rule_parameters'
    }

    def __init__(self, rule_id=None, name=None, description=None, category=None, rule_parameters=None):  # noqa: E501
        """CanaryRule - a model defined in Swagger"""  # noqa: E501
        self._rule_id = None
        self._name = None
        self._description = None
        self._category = None
        self._rule_parameters = None
        self.discriminator = None
        if rule_id is not None:
            self.rule_id = rule_id
        if name is not None:
            self.name = name
        if description is not None:
            self.description = description
        if category is not None:
            self.category = category
        if rule_parameters is not None:
            self.rule_parameters = rule_parameters

    @property
    def rule_id(self):
        """Gets the rule_id of this CanaryRule.  # noqa: E501


        :return: The rule_id of this CanaryRule.  # noqa: E501
        :rtype: int
        """
        return self._rule_id

    @rule_id.setter
    def rule_id(self, rule_id):
        """Sets the rule_id of this CanaryRule.


        :param rule_id: The rule_id of this CanaryRule.  # noqa: E501
        :type: int
        """

        self._rule_id = rule_id

    @property
    def name(self):
        """Gets the name of this CanaryRule.  # noqa: E501


        :return: The name of this CanaryRule.  # noqa: E501
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """Sets the name of this CanaryRule.


        :param name: The name of this CanaryRule.  # noqa: E501
        :type: str
        """

        self._name = name

    @property
    def description(self):
        """Gets the description of this CanaryRule.  # noqa: E501


        :return: The description of this CanaryRule.  # noqa: E501
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """Sets the description of this CanaryRule.


        :param description: The description of this CanaryRule.  # noqa: E501
        :type: str
        """

        self._description = description

    @property
    def category(self):
        """Gets the category of this CanaryRule.  # noqa: E501


        :return: The category of this CanaryRule.  # noqa: E501
        :rtype: CanaryRuleCategory
        """
        return self._category

    @category.setter
    def category(self, category):
        """Sets the category of this CanaryRule.


        :param category: The category of this CanaryRule.  # noqa: E501
        :type: CanaryRuleCategory
        """

        self._category = category

    @property
    def rule_parameters(self):
        """Gets the rule_parameters of this CanaryRule.  # noqa: E501


        :return: The rule_parameters of this CanaryRule.  # noqa: E501
        :rtype: list[CanaryRuleRuleParameters]
        """
        return self._rule_parameters

    @rule_parameters.setter
    def rule_parameters(self, rule_parameters):
        """Sets the rule_parameters of this CanaryRule.


        :param rule_parameters: The rule_parameters of this CanaryRule.  # noqa: E501
        :type: list[CanaryRuleRuleParameters]
        """

        self._rule_parameters = rule_parameters

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
        if issubclass(CanaryRule, dict):
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
        if not isinstance(other, CanaryRule):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """Returns true if both objects are not equal"""
        return not self == other
