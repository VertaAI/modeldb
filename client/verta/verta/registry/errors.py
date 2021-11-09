class CustomModelError(Exception):
    """Exception to be raised in user-defined models.

    Messages of this class are caught in model containers and converted to
    HTTP errors with the specified HTTP code and message in the response body.

    Attributes
    ----------
    http_code : int
        The http code to be returned by a wrapping service.
    message : str
        The response message.
    """

    def __init__(self, http_code, message):
        self.http_code = http_code
        self.message = message
        super(CustomModelError, self).__init__(self.message)
