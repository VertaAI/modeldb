const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getConfig = require('../../getConfig');
const getDriver = require('../../helpers/getDriver');
const testEntityDescriptionUpdating = require('../shared/testEntityDescriptionUpdating');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const testExperimentRunCommentsCRUD = require('../shared/testExperimentRunCommentsCRUD');
const {
  deleteAllProjects,
  createExperimentRun,
} = require('../../helpers/userData');
const { testSuitRetry, testCaseRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const config = getConfig();

const navigateToExperimentRunPage = async (
  driver,
  { projectId, experimentRunId }
) => {
  await driver.get(
    routes.projectsRoutes.makeExperimentRunRoute({
      projectId,
      experimentRunId,
    }));
};

const navigateToExperimentRunPageWithWaitingLoading = async (
  driver,
  { projectId, experimentRunId }
) => {
  await navigateToExperimentRunPage(driver, { projectId, experimentRunId });
  await driver.wait(
    until.elementsLocated(By.css('[data-test=experiment-run]')),
    15000
  );
};

const defaultMock = {
  project: { name: 'project-1-12312' },
  experiment: { name: 'experiment-1-32232' },
  experimentRun: { name: 'experiment-run' },
};

describe('experiment run', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllProjects();
  });

  describe('displaying', () => {
    let driver;

    beforeEach(async () => {
      const createdExprRunInfo = await createExperimentRun(
        defaultMock.project,
        defaultMock.experiment,
        defaultMock.experimentRun
      );

      driver = await getDriver();
      await navigateToExperimentRunPage(driver, createdExprRunInfo);
    });

    afterEach(async () => driver.quit());

    it('should display experimentRun', async function() {
      this.retries(testCaseRetry);

      await driver.wait(
        until.elementLocated(By.css(`[data-test=experiment-run]`)),
        15000,
        `should display experimentRun`
      );
      const experimentRunName = await (await driver.wait(
        until.elementLocated(By.css(`[data-test=experiment-run-name]`)),
        15000,
        `should display experimentRun name`
      )).getText();
      assert.equal(experimentRunName, defaultMock.experimentRun.name);
    });
  });

  testEntityDescriptionUpdating({
    entityName: 'experiment-run',
    createEntities: experimentRuns =>
      createExperimentRun(
        defaultMock.project,
        defaultMock.experiment,
        experimentRuns[0]
      ),
    navigateToPage: navigateToExperimentRunPageWithWaitingLoading,
  });

  testEntityTagsCRUD({
    entityName: 'experiment-run',
    createEntities: experimentRuns =>
      createExperimentRun(
        defaultMock.project,
        defaultMock.experiment,
        experimentRuns[0]
      ),
    navigateToPage: navigateToExperimentRunPageWithWaitingLoading,
  });

  testExperimentRunCommentsCRUD({
    navigateToPage: navigateToExperimentRunPageWithWaitingLoading,
  });

  describe('deleting', () => {
    let driver;
    let createdExprRunInfo;

    beforeEach(async () => {
      createdExprRunInfo = await createExperimentRun(
        defaultMock.project,
        defaultMock.experiment,
        defaultMock.experimentRun
      );

      driver = await getDriver();
      await navigateToExperimentRunPageWithWaitingLoading(
        driver,
        createdExprRunInfo
      );
    });

    afterEach(async () => driver.quit());

    it('should delete experimentRun', async function() {
      this.retries(testCaseRetry);

      const deleteButton = await driver.findElement(
        By.css(`[data-test=delete-experiment-run-button]`)
      );
      assert.exists(deleteButton, `experiment-run should has delete button`);
      await deleteButton.click();

      assert.exists(
        await driver.findElement(By.css('[data-test=confirm]')),
        'should show confirm modal for deleting'
      );
      await driver.findElement(By.css('[data-test=confirm-ok-button]')).click();

      await driver.sleep(1000);
      const experimentRunsPage = routes.projectsRoutes.makeExperimentRunsRoute({
        projectId: createdExprRunInfo.projectId,
      });
      await driver.wait(
        until.urlContains(experimentRunsPage),
        15000,
        'should redirect on exprRuns page after deleting'
      );
    });
  });
});
