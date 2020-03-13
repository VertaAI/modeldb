# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class RuntimeError(dict):
  def __init__(self, error=None, code=None, message=None, details=None):
    self.error = error
    self.code = code
    self.message = message
    self.details = details

  def __setattr__(self, name, value):
    self[name] = value

  def __delattr__(self, name):
    del self[name]

  def __getattr__(self, name):
    if name in self:
      return self[name]
    else:
      raise AttributeError
