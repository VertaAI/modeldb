const { By, Key, until } = require('selenium-webdriver');
const { assert } = require('chai');

const {
  addFilterByInput,
  InputFilterTypes,
} = require('../../../helpers/pageObjects/filters');
const getDriver = require('../../../helpers/getDriver');
const { testSuitRetry, testCaseRetry } = require('../testRetrySettings');

// test for entities which support filtering only by description, name, tags!
const testEntitiesFiltering = async ({
  entityName,
  createEntities,
  navigateToPage,
}) => {
  describe(`${entityName}s filtering`, function() {
    this.retries(testSuitRetry);
    const getDisplayedEntitiesInfo = async driver => {
      const displayedEntitiesNamesElems = await driver.findElements(
        By.css(`[data-test=${entityName}-name]`)
      );
      const displayedEntitiesNames = await Promise.all(
        displayedEntitiesNamesElems.map(entityName => entityName.getText())
      );
      return displayedEntitiesNames.map(entityName => ({ name: entityName }));
    };

    let driver;

    const dataForTest = [
      {
        isFiltered: true,
        entity: {
          name: `${entityName}-1-3123`,
          description: `${entityName}1-description`,
          tags: ['common-tag'],
        },
      },
      {
        isFiltered: true,
        entity: {
          name: `${entityName}-3-3123`,
          description: `description-${entityName}3`,
          tags: ['common-tag'],
        },
      },
      {
        isFiltered: false,
        entity: {
          name: `${entityName}-2-3123`,
          description: `${entityName}2-description`,
          tags: [],
        },
      },
    ];

    beforeEach(async () => {
      const createEntitiesRes = await createEntities(
        dataForTest.map(({ entity }) => entity)
      );

      driver = await getDriver();
      await navigateToPage(driver, createEntitiesRes);
    });

    afterEach(async () => driver.quit());

    it('should correct apply filters', async function() {
      this.retries(testCaseRetry);
      await addFilterByInput(driver, InputFilterTypes.name, `${entityName}`);
      await addFilterByInput(
        driver,
        InputFilterTypes.description,
        'description'
      );
      await addFilterByInput(driver, InputFilterTypes.tags, 'common-tag');

      await driver.sleep(3000);
      const displayedEntitiesInfo = await getDisplayedEntitiesInfo(driver);
      assert.equal(
        displayedEntitiesInfo.length,
        dataForTest.filter(({ isFiltered }) => isFiltered).length
      );

      await driver.sleep(1000);
      const expectedEntities = dataForTest
        .filter(({ isFiltered }) => isFiltered)
        .map(({ entity }) => entity);

      await driver.sleep(1000);
      assert.isTrue(
        expectedEntities.every(expectedEntity =>
          displayedEntitiesInfo.some(
            displayedEntity => expectedEntity.name === displayedEntity.name
          )
        ),
        `should display ${entityName}s which match to filters`
      );
    });
  });
};

module.exports = testEntitiesFiltering;
