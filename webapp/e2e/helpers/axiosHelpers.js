const R = require('ramda');

const getConfig = require('../getConfig');
const config = getConfig();

const setupAxiosInstance = (axios, userInfo) => {
    axios.defaults.baseURL = `${config.backend.domain}/api`;
    axios.defaults.responseType = 'json';
    axios.defaults.headers = {
        'Grpc-metadata-email': userInfo.email,
        'Grpc-metadata-developer_key': userInfo.developerKey,
        'Grpc-metadata-source': 'PythonClient',
    };

    axios.interceptors.response.use(
        (config) => config,
        error => {
            const errorResponse = error.response || error;
            const requestInfo = JSON.stringify(R.pick(['url', 'method', 'data', 'params', 'headers'], errorResponse.config), undefined, 2);
            const responseInfo = JSON.stringify({
                status: errorResponse.status,
                statusText: errorResponse.statusText,
                data: errorResponse.data,
            }, undefined, 2);
            const message = `\nrequest=${requestInfo}\nresponse=${responseInfo}`;
            error.message = message;
            throw error;
        },
    );

    return axios;
};

module.exports = {
    setupAxiosInstance,
};
