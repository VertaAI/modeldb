export const removeHttp = function(link: string) {
  return link.replace(/^(http?:|)\/\//, '');
};

export const removeHttps = function(link: string) {
  return link.replace(/^(https?:|)\/\//, '');
};

export const removeProtocols = function(link: string) {
  return link.replace(/(^\w+:|^)\/\//, '');
};
