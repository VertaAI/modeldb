const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../helpers/getDriver');
const testEntityDescriptionUpdating = require('../shared/testEntityDescriptionUpdating');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const {
  deleteAllDatasets,
  createDatasetVersion,
} = require('../../helpers/userData');
const { testSuitRetry, testCaseRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const navigateToDatasetVersionPage = async (
  driver,
  { datasetId, datasetVersionId }
) => {
  await driver.get(
    routes.datasetsRoutes.makeDatasetVersionRoute({
      datasetId,
      datasetVersionId,
    })
  );
};

const navigateToDatasetVersionPageWithWaitingLoading = async (
  driver,
  { datasetId, datasetVersionId }
) => {
  await navigateToDatasetVersionPage(driver, { datasetId, datasetVersionId });
  await driver.wait(
    until.elementsLocated(By.css('[data-test=dataset-version]')),
    15000
  );
};

const defaultMock = {
  dataset: {
    name: 'dataset',
    dataset_type: 'RAW',
  },
  datasetVersion: {
    description:
      'this is the first version of the datset, hence it does not have a parent_id',
    tags: ['tag has limit 40', 'tag 2'],
    dataset_visibility: 'PRIVATE',
    dataset_type: 'RAW',
    attributes: [
      { key: 'attributekey1', value: 'attributevalue1' },
      { key: 'attributekey2', value: 1 },
    ],
    raw_dataset_version_info: {
      size: 3300,
      features: [
        'age',
        'capital-gain',
        'capital-loss',
        'hours-per-week',
        'workclass_local-gov',
        'workclass_private',
        'workclass_self-emp-inc',
        'workclass_self-emp-not-inc',
        'workclass_state-gov',
        'workclass_without-pay',
        'education_11th',
        'education_12th',
        'education_1st-4th',
        'education_5th-6th',
        'education_7th-8th',
        'education_9th',
        'education_assoc-acdm',
        'education_assoc-voc',
        'education_bachelors',
        'education_doctorate',
        'education_hs-grad',
        'education_masters',
        'education_preschool',
        'education_prof-school',
        'education_some-college',
        'relationship_not-in-family',
        'relationship_other-relative',
        'relationship_own-child',
        'relationship_unmarried',
        'relationship_wife',
        'occupation_armed-forces',
        'occupation_craft-repair',
        'occupation_exec-managerial',
        'occupation_farming-fishing',
        'occupation_handlers-cleaners',
        'occupation_machine-op-inspct',
        'occupation_other-service',
        'occupation_priv-house-serv',
        'occupation_prof-specialty',
        'occupation_protective-serv',
        'occupation_sales',
        'occupation_tech-support',
        'occupation_transport-moving',
        '>50k',
      ],
      num_records: 36178,
      object_path:
        '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05/train_data',
      checksum:
        '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
    },
  },
};

describe('dataset version', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllDatasets();
  });

  describe('displaying', () => {
    let driver;

    beforeEach(async () => {
      const createdDatasetVersionInfo = await createDatasetVersion(
        defaultMock.dataset,
        defaultMock.datasetVersion
      );

      driver = await getDriver();
      await navigateToDatasetVersionPage(driver, createdDatasetVersionInfo);
    });

    afterEach(async () => driver.quit());

    it('should display datasetVersion', async function() {
      this.retries(testCaseRetry);

      await driver.wait(
        until.elementLocated(By.css(`[data-test=dataset-version]`)),
        30000,
        `should display dataset version`
      );
      const datasetVersionName = await (await driver.wait(
        until.elementLocated(By.css(`[data-test=dataset-version-name]`)),
        30000,
        `should display dataset version name`
      )).getText();
      assert.exists(datasetVersionName);
    });
  });

  testEntityDescriptionUpdating({
    entityName: 'dataset-version',
    createEntities: datasetVersions =>
      createDatasetVersion(defaultMock.dataset, datasetVersions[0]),
    navigateToPage: navigateToDatasetVersionPageWithWaitingLoading,
  });

  testEntityTagsCRUD({
    entityName: 'dataset-version',
    createEntities: datasetVersions =>
      createDatasetVersion(defaultMock.dataset, datasetVersions[0]),
    navigateToPage: navigateToDatasetVersionPageWithWaitingLoading,
  });

  describe('deleting', () => {
    let driver;
    let datasetVersionInfo;

    beforeEach(async () => {
      datasetVersionInfo = await createDatasetVersion(
        defaultMock.dataset,
        defaultMock.datasetVersion
      );

      driver = await getDriver();
      await navigateToDatasetVersionPageWithWaitingLoading(
        driver,
        datasetVersionInfo
      );
    });

    afterEach(async () => driver.quit());

    it('should delete dataset version', async function() {
      this.retries(testCaseRetry);

      const deleteButton = await driver.wait(
        until.elementLocated(
          By.css('[data-test=delete-dataset-version-button]')
        ),
        15000,
        'should delete dataset version button'
      );
      assert.exists(deleteButton, `dataset version should has delete button`);

      await driver.sleep(1000);
      await deleteButton.click();

      assert.exists(
        await driver.findElement(By.css('[data-test=confirm]')),
        'should show confirm modal for deleting'
      );
      await driver.findElement(By.css('[data-test=confirm-ok-button]')).click();

      await driver.sleep(3000);

      const datasetVersionsPage = routes.datasetsRoutes.makeDatasetsRoute({ datasetId: datasetVersionInfo.datasetId });
      await driver.wait(
        until.urlContains(datasetVersionsPage),
        30000,
        'should redirect on dataset versions page after deleting'
      );
    });
  });
});
