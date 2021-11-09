class ModelError(Exception):
    """Exception to be raised in user-defined models.

    Messages of this class are caught in model containers and converted to
    HTTP errors with the specified HTTP code and message in the response body.

    Attributes
    ----------
    message : str
        The response message.
    http_code : int
        The http code to be returned by a wrapping service.
    """

    def __init__(self, message, http_code):
        self.message = message
        self.http_code = http_code
        super(ModelError, self).__init__(self.message)
