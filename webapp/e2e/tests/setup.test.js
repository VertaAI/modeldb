const { updateUsername } = require('../helpers/userData');
const getConfig = require('../getConfig');

const config = getConfig();

before(async function() {
  this.timeout(15000);
});
