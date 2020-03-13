const { By } = require('selenium-webdriver');
var authenticator = require('authenticator');

const getConfig = require('../getConfig');

const config = getConfig();

async function handleReauthorization(driver) {
  try {
    await driver.findElement(By.id('js-oauth-authorize-btn')).click();
  } catch (e) {
    return undefined;
  }
}

async function handle2FA(driver) {
  const page2FA = 'https://github.com/sessions/two-factor';
  const pageUrl = await driver.getCurrentUrl();
  if (pageUrl === page2FA) {
    var token = authenticator.generateToken(config.github2FASecret);
    await driver.findElement(By.css('input[id=otp]')).sendKeys(token);
    await driver.findElement(By.css('button[type=submit]')).click();
  }
}

module.exports = {
  handle2FA,
  handleReauthorization,
};
