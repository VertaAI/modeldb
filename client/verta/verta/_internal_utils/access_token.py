# -*- coding: utf-8 -*-


class AccessToken(object):
    def __init__(self, token):
        self.access_token = token

    def __repr__(self):
        return "AccessToken({})".format(self.access_token[8:])

    def headers(self):
        return {"Access-Token": self.access_token}
