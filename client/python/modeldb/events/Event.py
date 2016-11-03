"""
Stores Events on server.
"""
from ..thrift.modeldb import ttypes as modeldb_types

class Event:
    """
	Base Class for creating and storing Events.
    """
    def __init__(self):
        pass

    def sync(self, syncer):
        """
    	This function is implemented by other subclasses.
        """
        pass
