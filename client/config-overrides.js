const path = require('path');

module.exports = function override(config) {
  config.resolve = {
    ...config.resolve,
    modules: config.resolve.modules.concat(['src']),
  };

  return config;
};