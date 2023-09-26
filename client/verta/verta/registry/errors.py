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
    include_stacktrace : bool
        Whether or not to include the stacktrace in HTTP response contents.
    """

    def __init__(self, message, http_code, include_stacktrace=False):
        if http_code >= 500 or http_code < 400:
            raise ValueError("error codes must fall in the 4XX range")
        self.message = message
        self.http_code = http_code
        self.include_stacktrace = include_stacktrace
        super(ModelError, self).__init__(self.message)
