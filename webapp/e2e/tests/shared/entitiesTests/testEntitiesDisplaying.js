const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../../helpers/getDriver');
const { testSuitRetry, testCaseRetry } = require('../testRetrySettings');

const testEntitiesDisplaying = ({
  entityName,
  createEntities,
  navigateToPage,
}) => {
  describe(`${entityName}s displaying`, function() {
    const mocks = [
      { name: `${entityName}-1232323` },
      { name: `${entityName}-2232323` },
      { name: `${entityName}-323232323` },
    ];

    this.retries(testSuitRetry);
    let driver;

    beforeEach(async () => {
      const createEntitiesRes = await createEntities(mocks);

      driver = await getDriver();
      await navigateToPage(driver, createEntitiesRes);
    });

    afterEach(async () => driver.quit());

    it(`should display ${entityName}s`, async function() {
      this.retries(testCaseRetry);
      const displayedEntitiesElems = await driver.wait(
        until.elementsLocated(By.css(`[data-test=${entityName}]`)),
        30000,
        `should display ${entityName}s`
      );
      assert.equal(
        displayedEntitiesElems.length,
        mocks.length,
        `should display ${mocks.length} ${entityName}s`
      );

      const displayedEntitiesNames = await Promise.all(
        displayedEntitiesElems.map(enitityElem => enitityElem.getText())
      );
      for (const expectedEntity of mocks) {
        assert.exists(
          displayedEntitiesNames.includes(expectedEntity.name),
          `should display ${entityName} with name = ${expectedEntity.name}`
        );
      }
    });
  });
};

module.exports = testEntitiesDisplaying;
