# THIS FILE IS AUTO-GENERATED. DO NOT EDIT

class ProtobufAny(dict):
  def __init__(self, type_url=None, value=None):
    self.type_url = type_url
    self.value = value

  def __setattr__(self, name, value):
    self[name] = value

  def __delattr__(self, name):
    del self[name]

  def __getattr__(self, name):
    if name in self:
      return self[name]
    else:
      raise AttributeError
