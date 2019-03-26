const axios = require('axios');

const apiAddress = process.env.BACKEND_API_PROTOCOL + "://" + 
    process.env.BACKEND_API_DOMAIN + ":" + 
    process.env.BACKEND_API_PORT

module.exports = {
  getFromAPI : function(url, headers, query) {
    console.log("********************CALLING FUNCTION***************", url)

    return axios({
        method: 'get',
        url: apiAddress + url,
        headers: headers,
        params : query
    });
  }
}