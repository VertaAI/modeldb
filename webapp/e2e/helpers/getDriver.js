const { Builder, Capabilities } = require('selenium-webdriver');

const getDriver = async () => {
    let chromeCapabilities = Capabilities.chrome();

    chromeCapabilities.set("goog:chromeOptions", {
      args: [
        "--disable-web-security",
        "--start-maximized",
      ]
    });
    
    driver = await new Builder()
                .forBrowser("chrome")
                .withCapabilities(chromeCapabilities)
                .build();
    return driver;
};

module.exports = getDriver;
