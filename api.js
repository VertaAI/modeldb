const axios = require('axios');

const apiAddress = process.env.BACKEND_API_PROTOCOL + "://" + 
    process.env.BACKEND_API_DOMAIN + ":" + 
    process.env.BACKEND_API_PORT

module.exports = {
  getFromAPI : function(url, headers, query) {
    return axios.get(apiAddress + url, 
      {
        headers: headers,
        params : query
      });
  }
}