const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');
const faker = require('faker');

const getConfig = require('../../getConfig');
const getDriver = require('../../helpers/getDriver');
const entitiesTests = require('../shared/entitiesTests');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const {
  addFilterByInput,
  InputFilterTypes,
} = require('../../helpers/pageObjects/filters');
const {
  deleteAllDatasets,
  createDatasetVersions,
} = require('../../helpers/userData');
const { testSuitRetry, testCaseRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const defaultMock = {
  dataset: { name: 'dataset', dataset_type: 'RAW' },
};

const navigateToDatasetVersionsPage = async (driver, { datasetId }) => {
  await driver.get(routes.datasetsRoutes.makeDatasetVersionsRoute({
    datasetId
  }));
};

const navigateToDatasetVersionsPageWithWaitingLoadingEntities = async (
  driver,
  { datasetId }
) => {
  await navigateToDatasetVersionsPage(driver, { datasetId });
  await driver.wait(
    until.elementsLocated(By.css('[data-test=dataset-version]')),
    30000
  );
};

const createDefaultDatasetVersions = entities => {
  return createMockDatasetVersion('RAW', entities);
};

const createMockDatasetVersion = (type, entities) => {
  const getInfoByType = i => {
    switch (type) {
      case 'RAW':
        return {
          raw_dataset_version_info: {
            size: faker.random.number(),
            features: [
              faker.lorem.word(),
              faker.lorem.word(),
              faker.lorem.word(),
              faker.lorem.word(),
              faker.lorem.word(),
            ],
            num_records: faker.random.number(),
            object_path: faker.random.uuid(),
            checksum: faker.random.uuid(),
          },
        };
      case 'PATH':
        return {
          path_dataset_version_info: {
            location_type: 'S3_FILE_SYSTEM',
            size: faker.random.number(),
            dataset_part_infos: [
              {
                path:
                  'https://verta-condacon.s3.amazonaws.com/000929cc-4b56-46be-9e49-82f169a833e5/train_data',
                size: faker.random.number(),
                checksum: '64af2ff44dd04acceb277d024939b619',
                last_modified_at_source: 1559065471,
              },
            ],
            base_path: 'arn:aws:s3:::verta-condacon',
          },
        };
      case 'QUERY':
        return {
          query_dataset_version_info: {
            query:
              "select full_name, superpower, location from superheros where first_name = 'Kirill' and last_name = 'Gulyaev'",
            query_template:
              "select name, superpower, location from superheros where first_name = '$1' and last_name = '$2'",
            query_parameters: [
              {
                parameter_name: '$1',
                value: 'Kirill',
              },
              {
                parameter_name: '$2',
                value: 'Gulyaev',
              },
            ],
            data_source_uri:
              'jdbc:sqlserver://192.168.5.73:1234;databaseName=AdventureWorks;user=scott;password=p123',
            execution_timestamp: 1531265327,
            num_records: faker.random.number(),
          },
        };
    }
  };
  const datasetVersions = entities.map((entity, i) => ({
    ...entity,
    dataset_type: type,
    ...getInfoByType(i),
  }));
  return createDatasetVersions(
    { ...defaultMock.dataset, dataset_type: type },
    datasetVersions
  );
};

describe('dataset versions', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllDatasets();
  });

  ['RAW', 'QUERY', 'PATH'].forEach(type => {
    describe(`loading dataset versions with type = ${type}`, () => {
      entitiesTests.testEntitiesDisplaying({
        entityName: 'dataset-version',
        createEntities: entities => createMockDatasetVersion(type, entities),
        navigateToPage: navigateToDatasetVersionsPage,
      });
    });
  });

  entitiesTests.testEntityDeleting({
    entityName: 'dataset-version',
    createEntities: createDefaultDatasetVersions,
    navigateToPage: navigateToDatasetVersionsPageWithWaitingLoadingEntities,
  });

  entitiesTests.testEntitiesPagination({
    entityName: 'dataset-version',
    createEntities: createDefaultDatasetVersions,
    navigateToPage: navigateToDatasetVersionsPageWithWaitingLoadingEntities,
    pageSize: 20,
  });

  describe.skip('tags CRUD', () => {
    testEntityTagsCRUD({
      entityName: 'dataset-version',
      createEntities: createDefaultDatasetVersions,
      navigateToPage: navigateToDatasetVersionsPageWithWaitingLoadingEntities,
    });
  });

  describe('filtering', () => {
    const dataForTest = [
      { isFiltered: true, entity: { tags: ['common-tag'] } },
      { isFiltered: true, entity: { tags: ['common-tag'] } },
      { isFiltered: false, entity: { tags: [] } },
    ];

    const getDisplayedDatasetVersionsInfo = async driver => {
      const displayedDatasetVersionsNamesElems = await driver.findElements(
        By.css(`[data-test=dataset-version-name]`)
      );
      const displayedDatasetVersionsNames = await Promise.all(
        displayedDatasetVersionsNamesElems.map(entityName =>
          entityName.getText()
        )
      );
      return displayedDatasetVersionsNames.map(entityName => ({
        name: entityName,
      }));
    };

    beforeEach(async () => {
      const createDatasetVersionsRes = await createDefaultDatasetVersions(
        dataForTest.map(({ entity }) => entity)
      );

      driver = await getDriver();
      await navigateToDatasetVersionsPageWithWaitingLoadingEntities(
        driver,
        createDatasetVersionsRes
      );
    });

    afterEach(async () => driver.quit());

    it('should correct apply filters', async function() {
      this.retries(testCaseRetry);

      await addFilterByInput(driver, InputFilterTypes.tags, 'common-tag');

      await driver.sleep(3000);

      const displayedEntitiesInfo = await getDisplayedDatasetVersionsInfo(
        driver
      );
      const expectedEntities = dataForTest
        .filter(({ isFiltered }) => isFiltered)
        .map(({ entity }) => entity);
      await driver.sleep(1000);
      assert.equal(displayedEntitiesInfo.length, expectedEntities.length);
    });
  });

  // describe('compare dataset versions', () => {
  //   let driver;
  //   let createdDatasetVersions;

  //   const getDisplayedEntitiesInfo = async (driver, entityName) => {
  //     const displayedEntitiesIdsElems = await driver.findElements(
  //       By.css(`[data-test^=${entityName}-id]`)
  //     );
  //     const displayedEntitiesIds = await Promise.all(
  //       displayedEntitiesIdsElems.map(entityId => entityId.getText())
  //     );
  //     return displayedEntitiesIds.map(entityId => ({ id: entityId }));
  //   };
  //   const getDatasetVersionElem = async (driver, id) => {
  //     const datasetVersionsElems = await driver.findElements(
  //       By.css('[data-test=dataset-version]')
  //     );
  //     const datasetVersionsInfo = await getDisplayedEntitiesInfo(
  //       driver,
  //       'dataset-version'
  //     );
  //     const targetDatasetVersion =
  //       datasetVersionsElems[
  //         datasetVersionsInfo.findIndex(exprRun => exprRun.id === id)
  //       ];
  //     return targetDatasetVersion;
  //   };

  //   beforeEach(async () => {
  //     createdDatasetVersions = await createDefaultDatasetVersions([
  //       { version: '111' },
  //       { version: '2222' },
  //     ]);

  //     driver = await getDriver();
  //     await navigateToDatasetVersionsPageWithWaitingLoadingEntities(
  //       driver,
  //       createdDatasetVersions
  //     );
  //   });

  //   afterEach(async () => driver.quit());

  //   it('should open "comparing dataset versions" page for selected dataset versions', async () => {
  //     await getDatasetVersionElem(
  //       driver,
  //       createdDatasetVersions.datasetVersions[0].id
  //     )
  //       .then(e => e.findElement(By.css('[data-test=comparing-toggler]')))
  //       .then(e => e.click());
  //     await getDatasetVersionElem(
  //       driver,
  //       createdDatasetVersions.datasetVersions[1].id
  //     )
  //       .then(e => e.findElement(By.css('[data-test=comparing-toggler]')))
  //       .then(e => e.click());

  //     const entitiesForComparing = await driver.findElements(
  //       By.css('[data-test=entity-for-comparing]')
  //     );

  //     assert.equal(
  //       entitiesForComparing.length,
  //       2,
  //       'should display ids of dataset versions which are selected for comparing'
  //     );

  //     await driver
  //       .findElement(By.css('[data-test=compare-entities-button]'))
  //       .click();

  //     await driver.sleep(1000);
  //     const expectedUrl = `${config.baseURL}/datasets/${
  //       createdDatasetVersions.datasetId
  //     }/versions/compare/${createdDatasetVersions.datasetVersions[0].id}/${
  //       createdDatasetVersions.datasetVersions[1].id
  //     }`;
  //     await driver.wait(
  //       until.urlIs(expectedUrl),
  //       30000,
  //       'should redirect on "compare dataset versions" page'
  //     );
  //   });
  // });
});
