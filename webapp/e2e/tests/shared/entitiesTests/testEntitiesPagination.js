const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../../helpers/getDriver');
const { testSuitRetry, testCaseRetry } = require('../testRetrySettings');

const testEntitiesPagination = ({
  entityName,
  createEntities,
  navigateToPage,
  pageSize,
}) => {
  describe(`${entityName} pagination`, function() {
    this.retries(testSuitRetry);
    const entities = Array.from({ length: pageSize * 3 }).map((_, i) => ({
      name: `${entityName}-${i}-32`,
    }));
    const pagesCount = Math.ceil(entities.length / pageSize);

    const getPaginationPages = async driver => {
      return await driver.findElements(By.css('.pagination-page'));
    };

    let driver;

    beforeEach(async () => {
      const createEntitiesRes = await createEntities(entities);

      driver = await getDriver();
      await navigateToPage(driver, createEntitiesRes);
    });

    afterEach(async () => driver.quit());

    const getDisplayedEntitiesInfo = async driver => {
      const displayedEntitiesNamesElems = await driver.findElements(
        By.css(`[data-test=${entityName}-name]`)
      );
      const displayedEntitiesNames = await Promise.all(
        displayedEntitiesNamesElems.map(entityName => entityName.getText())
      );
      return displayedEntitiesNames.map(entityName => ({ name: entityName }));
    };

    it(`should correct display ${entityName}s and pagination pages count on 1 page`, async function() {
      this.retries(testCaseRetry);
      const entities = await driver.wait(
        until.elementsLocated(By.css(`[data-test=${entityName}]`)),
        30000
      );
      await driver.sleep(4000);
      assert.equal(
        entities.length,
        pageSize,
        `should display ${pageSize} on 1 page`
      );
      await driver.sleep(1000);
      const actualPaginationPages = await getPaginationPages(driver);
      await driver.sleep(1000);
      assert.equal(
        actualPaginationPages.length,
        pagesCount,
        `should display ${pagesCount} pages count`
      );
    });

    it(`should correct change pagination page`, async function() {
      this.retries(testCaseRetry);
      await driver.wait(
        until.elementsLocated(By.css(`[data-test=${entityName}]`)),
        30000
      );
      const entitiesInfoOn1Page = await getDisplayedEntitiesInfo(driver);

      const paginationPages = await getPaginationPages(driver);
      // select 2 page
      await paginationPages[1].click();

      await driver.sleep(4000);
      const entitiesInfoOn2Page = await getDisplayedEntitiesInfo(driver);
      assert.equal(
        entitiesInfoOn2Page.length,
        pageSize,
        `should display ${pageSize} on 2 page`
      );
      await driver.sleep(1000);
      assert.notSameDeepOrderedMembers(
        entitiesInfoOn1Page,
        entitiesInfoOn2Page,
        `should display correct ${entityName}s for 2 page`
      );
    });
  });
};

module.exports = testEntitiesPagination;
