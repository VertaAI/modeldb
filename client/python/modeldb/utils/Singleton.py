"""
Implements an unorthodox Singleton metaclass to inherit from.inherit
If an instance of the class already exists, replace it instead of
returning the old instance.
"""


class Singleton(type):
    _instances = {}

    def __call__(cls, *args, **kwargs):
        cls._instances[cls] = super(
            Singleton, cls).__call__(*args, **kwargs)
        return cls._instances[cls]

        # The code below would return the old class
        # instead creating a new one
        # if cls not in cls._instances:
        #     cls._instances[cls] = super(
        #         Singleton, cls).__call__(*args, **kwargs)
        # return cls._instances[cls]
