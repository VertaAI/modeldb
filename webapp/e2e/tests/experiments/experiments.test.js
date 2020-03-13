const { By, until } = require('selenium-webdriver');

const entitiesTests = require('../shared/entitiesTests');
const testEntityDescriptionUpdating = require('../shared/testEntityDescriptionUpdating');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const { deleteAllProjects, createProjectExperiments } = require('./mocks');
const { testSuitRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const defaultMock = {
  project: { name: 'project-with-experiments' },
  experiments: [{ name: 'experiment-1' }, { name: 'experiment-2' }],
};

const navigateToProjectExperimentsPage = async (driver, projectId) => {
  await driver.get(routes.projectsRoutes.makeExperimentsRoute({ projectId }));
};

const navigateToProjectExperimentsPageWithWaitingLoadingEntities = async (
  driver,
  projectId
) => {
  await navigateToProjectExperimentsPage(driver, projectId);
  await driver.wait(
    until.elementsLocated(By.css('[data-test=experiment]')),
    30000
  );
};

describe('experiments', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllProjects();
  });

  entitiesTests.testEntitiesDisplaying({
    entityName: 'experiment',
    createEntities: experiments =>
      createProjectExperiments(defaultMock.project, experiments),
    navigateToPage: navigateToProjectExperimentsPage,
  });

  entitiesTests.testWidgetEntitiesDeleting({
    entityName: 'experiment',
    createEntities: experiments =>
      createProjectExperiments(defaultMock.project, experiments),
    navigateToPage: navigateToProjectExperimentsPageWithWaitingLoadingEntities,
  });

  entitiesTests.testEntitiesFiltering({
    entityName: 'experiment',
    createEntities: experiments =>
      createProjectExperiments(defaultMock.project, experiments),
    navigateToPage: navigateToProjectExperimentsPageWithWaitingLoadingEntities,
  });

  entitiesTests.testEntitiesPagination({
    entityName: 'experiment',
    createEntities: experiments =>
      createProjectExperiments(defaultMock.project, experiments),
    pageSize: 10,
    navigateToPage: navigateToProjectExperimentsPageWithWaitingLoadingEntities,
  });

  testEntityDescriptionUpdating({
    entityName: 'experiment',
    createEntities: experiments =>
      createProjectExperiments(defaultMock.project, experiments),
    navigateToPage: navigateToProjectExperimentsPageWithWaitingLoadingEntities,
  });

  testEntityTagsCRUD({
    entityName: 'experiment',
    createEntities: experiments =>
      createProjectExperiments(defaultMock.project, experiments),
    navigateToPage: navigateToProjectExperimentsPageWithWaitingLoadingEntities,
  });
});
