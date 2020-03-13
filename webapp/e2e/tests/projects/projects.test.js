const entitiesTests = require('../shared/entitiesTests');
const testEntityDescriptionUpdating = require('../shared/testEntityDescriptionUpdating');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const { createProjects, deleteAllProjects } = require('../../helpers/userData');
const { testSuitRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const navigateToProjectsPage = async (driver) => {
  await driver.get(routes.projectsRoutes.makeProjectsRoute());
};

describe('projects', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllProjects();
  });

  entitiesTests.testEntitiesDisplaying({
    entityName: 'project',
    createEntities: createProjects,
    navigateToPage: navigateToProjectsPage,
  });

  entitiesTests.testWidgetEntitiesDeleting({
    entityName: 'project',
    createEntities: createProjects,
    navigateToPage: navigateToProjectsPage,
  });

  entitiesTests.testEntitiesFiltering({
    entityName: 'project',
    createEntities: createProjects,
    navigateToPage: navigateToProjectsPage,
  });

  entitiesTests.testEntitiesPagination({
    entityName: 'project',
    pageSize: 10,
    createEntities: createProjects,
    navigateToPage: navigateToProjectsPage,
  });

  testEntityDescriptionUpdating({
    entityName: 'project',
    createEntities: createProjects,
    navigateToPage: navigateToProjectsPage,
  });
  
  testEntityTagsCRUD({
    entityName: 'project',
    createEntities: createProjects,
    navigateToPage: navigateToProjectsPage,
  });
});
