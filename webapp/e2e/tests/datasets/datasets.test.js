const { By, until } = require('selenium-webdriver');

const entitiesTests = require('../shared/entitiesTests');
const testEntityDescriptionUpdating = require('../shared/testEntityDescriptionUpdating');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const { createDatasets, deleteAllDatasets } = require('../../helpers/userData');
const { testSuitRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const navigateToDatasetsPage = async driver => {
  await driver.get(routes.datasetsRoutes.makeDatasetsRoute());
};

const navigateToDatasetsPageWithWaitingLoadingEntities = async (
  driver,
  projectId
) => {
  await navigateToDatasetsPage(driver, projectId);
  await driver.wait(
    until.elementsLocated(By.css('[data-test=dataset]')),
    30000
  );
};

describe('datasets', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllDatasets();
  });

  entitiesTests.testEntitiesDisplaying({
    entityName: 'dataset',
    createEntities: createDatasets,
    navigateToPage: navigateToDatasetsPage,
  });

  entitiesTests.testWidgetEntitiesDeleting({
    entityName: 'dataset',
    createEntities: createDatasets,
    navigateToPage: navigateToDatasetsPageWithWaitingLoadingEntities,
  });

  entitiesTests.testEntitiesFiltering({
    entityName: 'dataset',
    createEntities: createDatasets,
    navigateToPage: navigateToDatasetsPageWithWaitingLoadingEntities,
  });

  entitiesTests.testEntitiesPagination({
    entityName: 'dataset',
    pageSize: 5,
    createEntities: createDatasets,
    navigateToPage: navigateToDatasetsPageWithWaitingLoadingEntities,
  });

  testEntityDescriptionUpdating({
    entityName: 'dataset',
    createEntities: createDatasets,
    navigateToPage: navigateToDatasetsPageWithWaitingLoadingEntities,
  });

  testEntityTagsCRUD({
    entityName: 'dataset',
    createEntities: createDatasets,
    navigateToPage: navigateToDatasetsPageWithWaitingLoadingEntities,
  });
});
