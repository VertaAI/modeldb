const axios = require('axios');

const apiAddress = `${process.env.BACKEND_API_PROTOCOL}://${process.env.BACKEND_API_DOMAIN}${
  process.env.BACKEND_API_PORT ? `:${process.env.BACKEND_API_PORT}` : ''
}`;

module.exports = {
  getFromAPI : function(url, method, headers, query, data) {
    return axios(apiAddress + url, 
      {
        method: method.toLowerCase(),
        headers: headers,
        params : query,
        data: data,
      });
  }
}