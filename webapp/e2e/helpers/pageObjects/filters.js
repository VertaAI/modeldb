const { By, Key, until } = require('selenium-webdriver');
const { assert } = require('chai');

const addFilterByInput = async (driver, filterType, filterValue) => {
    await driver.wait(until.elementLocated(By.css('[data-test=filters]')), 15000, 'should display filter bar');
    const quickFilterInputEl = await driver.wait(until.elementLocated(By.css('[data-test=quick-filter-input]')), 10000, 'should display input for quick filters');
    await quickFilterInputEl.click();

    const targetQuickFilterItemElem = await driver.findElement(By.css(`[data-test=quick-filter-item-${filterType}]`));
    await targetQuickFilterItemElem.click();

    await quickFilterInputEl.sendKeys(filterValue);
    await quickFilterInputEl.sendKeys(Key.ENTER);
};

const InputFilterTypes = {
    tags: 'tags',
    description: 'description',
    name: 'name',
};

module.exports = {
    addFilterByInput,
    InputFilterTypes,
};
