"""
Stores Events on server.
"""


class Event:
    """
        Base Class for creating and storing Events.
    """

    def __init__(self):
        pass

    def make_event(self, syncer):
        '''
        Create a ModelDB event from the data stored in this event.
        Functionality specific to the syncer (e.g. requiring the format of
        dataframes say, should be delegated to the syncer)
        '''
        return None

    def sync(self, syncer):
        """
        This function is implemented by other subclasses.
        """
        pass
