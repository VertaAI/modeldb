const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../../helpers/getDriver');
const { testSuitRetry, testCaseRetry } = require('../testRetrySettings');

const testEntityDeleting = ({ entityName, createEntities, navigateToPage }) => {
  describe(`${entityName} deleting`, function() {
    this.retries(testSuitRetry);
    let driver;

    beforeEach(async () => {
      const createEntitiesRes = await createEntities([
        { name: `${entityName}1` },
        { name: `${entityName}2` },
      ]);

      driver = await getDriver();
      await navigateToPage(driver, createEntitiesRes);
    });

    afterEach(async () => driver.quit());

    it(`should delete ${entityName}`, async function() {
      this.retries(testCaseRetry);
      const entitiesBeforeDeleting = await driver.wait(
        until.elementsLocated(By.css(`[data-test=${entityName}]`)),
        30000
      );

      const deleteButton = await driver.findElement(
        By.css(`[data-test=delete-${entityName}-button]`)
      );
      assert.exists(deleteButton, `${entityName} should has delete button`);
      await driver.sleep(1000);
      await deleteButton.click();

      await driver.sleep(4000);
      assert.exists(
        await driver.findElement(By.css('[data-test=confirm]')),
        'should show confirm modal for deleting'
      );
      await driver.findElement(By.css('[data-test=confirm-ok-button]')).click();

      await driver.sleep(4000);
      const entitiesAfterDeleting = await driver.wait(
        until.elementsLocated(By.css(`[data-test=${entityName}]`)),
        15000
      );
      assert.isTrue(
        entitiesBeforeDeleting.length - entitiesAfterDeleting.length === 1,
        `should delete ${entityName}`
      );
    });
  });
};

module.exports = testEntityDeleting;
