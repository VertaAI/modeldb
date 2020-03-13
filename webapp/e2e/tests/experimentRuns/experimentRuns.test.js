const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../helpers/getDriver');
const getConfig = require('../../getConfig');
const entitiesTests = require('../shared/entitiesTests');
const testEntityTagsCRUD = require('../shared/testEntityTagsCRUD');
const testExperimentRunCommentsCRUD = require('../shared/testExperimentRunCommentsCRUD');
const {
  deleteAllProjects,
  createExperimentRuns,
} = require('../../helpers/userData');
const { mockExperimentRuns } = require('./mocks');
const { testSuitRetry, testCaseRetry } = require('../shared/testRetrySettings');
const routes = require('../../helpers/routes');

const config = getConfig();

const defaultMock = {
  project: { name: 'project-4134' },
  experiment: { name: 'experiment' },
  experimentRuns: mockExperimentRuns,
};

const navigateToExperimentRunsPage = async (driver, { projectId }) => {
  await driver.get(routes.projectsRoutes.makeExperimentRunsRoute({ projectId }));
};

const navigateToExperimentRunsPageWithWaitingLoadingEntities = async (
  driver,
  projectId
) => {
  await navigateToExperimentRunsPage(driver, projectId);
  await driver.wait(
    until.elementsLocated(By.css('[data-test=experiment-run]')),
    30000
  );
};

const getDisplayedEntitiesInfo = async (driver, entityName) => {
  const displayedEntitiesNamesElems = await driver.findElements(
    By.css(`[data-test=${entityName}-name]`)
  );
  const displayedEntitiesNames = await Promise.all(
    displayedEntitiesNamesElems.map(entityName => entityName.getText())
  );
  return displayedEntitiesNames.map(entityName => ({ name: entityName }));
};

const getExperimentRunElem = async (driver, experimentRunName) => {
  const experimentRunsElems = await driver.findElements(
    By.css('[data-test=experiment-run]')
  );
  const experimentRunsInfo = await getDisplayedEntitiesInfo(
    driver,
    'experiment-run'
  );
  const targetExperimentRun =
    experimentRunsElems[
      experimentRunsInfo.findIndex(
        exprRun => exprRun.name === experimentRunName
      )
    ];
  return targetExperimentRun;
};

describe('experiment runs', function() {
  this.timeout(60000);
  this.retries(testSuitRetry);

  beforeEach(async () => {
    await deleteAllProjects();
  });

  entitiesTests.testEntitiesDisplaying({
    entityName: 'experiment-run',
    createEntities: experimentRuns =>
      createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        experimentRuns
      ),
    navigateToPage: navigateToExperimentRunsPage,
  });

  entitiesTests.testEntityDeleting({
    entityName: 'experiment-run',
    createEntities: experimentRuns =>
      createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        experimentRuns
      ),
    navigateToPage: navigateToExperimentRunsPageWithWaitingLoadingEntities,
  });

  entitiesTests.testEntitiesPagination({
    entityName: 'experiment-run',
    createEntities: experimentRuns =>
      createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        experimentRuns
      ),
    pageSize: 10,
    navigateToPage: navigateToExperimentRunsPageWithWaitingLoadingEntities,
  });

  testEntityTagsCRUD({
    entityName: 'experiment-run',
    createEntities: experimentRuns =>
      createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        experimentRuns
      ),
    navigateToPage: navigateToExperimentRunsPageWithWaitingLoadingEntities,
  });

  describe('filtering', () => {
    this.retries(testSuitRetry);
    // helpers
    const addFilterById = async (driver, { experimentRunName }) => {
      const targetExperimentRun = await getExperimentRunElem(
        driver,
        experimentRunName
      );
      await driver.sleep(6000);
      await targetExperimentRun
        .findElement(By.css('[data-test=toggle-filter-by-id]'))
        .click();
    };

    const addFilterByDnd = async (driver, { experimentRunName, type, key }) => {
      function addFilterByDndScript({ experimentRunName, type, key }) {
        function simulateDragDrop(sourceNode, destinationNode) {
          var EVENT_TYPES = {
            DRAG_END: 'dragend',
            DRAG_START: 'dragstart',
            DROP: 'drop',
          };

          function createCustomEvent(type) {
            var event = new CustomEvent('CustomEvent');
            event.initCustomEvent(type, true, true, null);
            event.dataTransfer = {
              data: {},
              setData: function(type, val) {
                this.data[type] = val;
              },
              getData: function(type) {
                return this.data[type];
              },
            };
            return event;
          }

          function dispatchEvent(node, type, event) {
            if (node.dispatchEvent) {
              return node.dispatchEvent(event);
            }
            if (node.fireEvent) {
              return node.fireEvent('on' + type, event);
            }
          }

          var event = createCustomEvent(EVENT_TYPES.DRAG_START);
          dispatchEvent(sourceNode, EVENT_TYPES.DRAG_START, event);

          var dropEvent = createCustomEvent(EVENT_TYPES.DROP);
          dropEvent.dataTransfer = event.dataTransfer;
          dispatchEvent(destinationNode, EVENT_TYPES.DROP, dropEvent);

          var dragEndEvent = createCustomEvent(EVENT_TYPES.DRAG_END);
          dragEndEvent.dataTransfer = event.dataTransfer;
          dispatchEvent(sourceNode, EVENT_TYPES.DRAG_END, dragEndEvent);
        }

        const targetKeyValueElem = Array.from(
          document.querySelectorAll(`[data-test=experiment-run]`)
        )
          .map(experimentRunElem => experimentRunElem.closest('tr'))
          .filter(
            experimentRunElem =>
              experimentRunElem.querySelector(`[data-test=experiment-run-name]`)
                .textContent === experimentRunName
          )
          .map(targetExperimentRunElem =>
            Array.from(
              targetExperimentRunElem.querySelectorAll(
                `[data-test=${type}-key]`
              )
            ).find(keyValueElem => keyValueElem.textContent === key)
          )[0];

        simulateDragDrop(
          targetKeyValueElem,
          document.querySelector('[data-test=filter-items-area]')
        );
      }

      // need use script because .actions().dragAndDrop() doesn't work
      await driver.executeScript(addFilterByDndScript, {
        experimentRunName,
        type,
        key,
      });
    };

    let driver;

    beforeEach(async () => {
      const createdExprRunInfo = await createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        defaultMock.experimentRuns
      );

      driver = await getDriver();
      await navigateToExperimentRunsPageWithWaitingLoadingEntities(
        driver,
        createdExprRunInfo
      );
    });

    afterEach(async () => driver.quit());

    const testCases = [
      {
        filters: [
          {
            filterType: 'keyValue',
            experimentRunName: defaultMock.experimentRuns[0].name,
            type: 'metric',
            key: 'val_acc',
          },
        ],
        expected: [
          defaultMock.experimentRuns[0],
          defaultMock.experimentRuns[3],
        ],
      },
      {
        filters: [
          {
            filterType: 'keyValue',
            experimentRunName: defaultMock.experimentRuns[0].name,
            type: 'hyperparameter',
            key: 'max_iter',
          },
        ],
        expected: [
          defaultMock.experimentRuns[0],
          defaultMock.experimentRuns[2],
        ],
      },
      {
        filters: [
          {
            filterType: 'keyValue',
            experimentRunName: defaultMock.experimentRuns[0].name,
            type: 'metric',
            key: 'val_acc',
          },
          {
            filterType: 'keyValue',
            experimentRunName: defaultMock.experimentRuns[0].name,
            type: 'hyperparameter',
            key: 'max_iter',
          },
        ],
        expected: [defaultMock.experimentRuns[0]],
      },
      {
        filters: [
          {
            filterType: 'id',
            experimentRunName: defaultMock.experimentRuns[0].name,
          },
        ],
        expected: [defaultMock.experimentRuns[0]],
      },
      {
        filters: [
          {
            filterType: 'id',
            experimentRunName: defaultMock.experimentRuns[0].name,
          },
          {
            filterType: 'id',
            experimentRunName: defaultMock.experimentRuns[1].name,
          },
        ],
        expected: [
          defaultMock.experimentRuns[0],
          defaultMock.experimentRuns[1],
        ],
      },
      {
        filters: [
          {
            filterType: 'id',
            experimentRunName: defaultMock.experimentRuns[0].name,
          },
          {
            filterType: 'id',
            experimentRunName: defaultMock.experimentRuns[1].name,
          },
          {
            filterType: 'keyValue',
            experimentRunName: defaultMock.experimentRuns[0].name,
            type: 'metric',
            key: 'val_acc',
          },
        ],
        expected: [defaultMock.experimentRuns[0]],
      },
    ];

    testCases.forEach(testCase => {
      it(`should filter experiment runs with filters ${JSON.stringify(
        testCase.filters,
        undefined,
        2
      )}`, async function() {
        this.retries(testCaseRetry);

        if (testCase.filters.some(({ filterType }) => filterType === 'id')) {
          for (const filter of testCase.filters.filter(
            ({ filterType }) => filterType === 'id'
          )) {
            await driver.sleep(3000);
            await addFilterById(driver, filter);
          }
          await driver.sleep(3000);
          await driver
            .findElement(By.css('[data-test=apply-ids-filter-button]'))
            .click();
          await driver.sleep(3000);
        }
        for (const filter of testCase.filters.filter(
          ({ filterType }) => filterType === 'keyValue'
        )) {
          await addFilterByDnd(driver, filter);
          await driver.sleep(3000);
        }

        const displayedEntitiesInfo = await getDisplayedEntitiesInfo(
          driver,
          'experiment-run'
        );
        for (const expectedExperimentRun of testCase.expected) {
          const found = displayedEntitiesInfo.find(
            displayEntitiesInfo =>
              expectedExperimentRun.name === displayEntitiesInfo.name
          );
          assert.exists(
            found,
            `should display experimentRun with name = ${
              expectedExperimentRun.name
            }`
          );
        }
      });
    });
  });

  describe('sorting', () => {
    this.retries(testSuitRetry);
    let driver;

    beforeEach(async () => {
      const createdExprRunInfo = await createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        defaultMock.experimentRuns
      );

      driver = await getDriver();
      await navigateToExperimentRunsPageWithWaitingLoadingEntities(
        driver,
        createdExprRunInfo
      );
    });

    afterEach(async () => driver.quit());

    // todo improve
    const addSorting = async (driver, { key, direction }) => {
      const fieldSortSelection = await driver.wait(
        until.elementLocated(By.css('[data-test=select-field-sorting]')),
        15000,
        'should click sort button'
      );
      await fieldSortSelection.click();

      await driver.sleep(3000);
      const fieldSortingItems = await driver.findElements(
        By.css('[data-test=select-field-sorting-item]')
      );

      await driver.sleep(1000);
      for (const fieldSortingItem of fieldSortingItems) {
        if ((await fieldSortingItem.getText()).includes(key)) {
          await fieldSortingItem
            .findElement(
              By.css(`[data-test=select-field-item-${direction}-direction]`)
            )
            .click();
        }
      }
    };

    it('should sort experiment runs', async function() {
      this.retries(testCaseRetry);

      await addSorting(driver, {
        type: 'metric',
        key: 'val_acc',
        direction: 'asc',
      });

      await driver.sleep(3000);

      const displayedEntitiesInfo = await getDisplayedEntitiesInfo(
        driver,
        'experiment-run'
      );
      const expected = [
        defaultMock.experimentRuns[1],
        defaultMock.experimentRuns[2],
        defaultMock.experimentRuns[0],
        defaultMock.experimentRuns[3],
      ];
      await driver.sleep(1000);
      expected.forEach((expectedExperimentRun, i) => {
        assert.equal(
          expectedExperimentRun.name,
          displayedEntitiesInfo[i].name,
          `should display experimentRun ${
            expectedExperimentRun.name
          } on ${i} pos`
        );
      });
    });
  });

  describe('compare experiment runs', () => {
    this.retries(testSuitRetry);
    let driver;
    let createdExprRunsInfo;

    beforeEach(async () => {
      createdExprRunsInfo = await createExperimentRuns(
        defaultMock.project,
        defaultMock.experiment,
        defaultMock.experimentRuns
      );

      driver = await getDriver();
      await navigateToExperimentRunsPageWithWaitingLoadingEntities(
        driver,
        createdExprRunsInfo
      );
    });

    afterEach(async () => driver.quit());

    it('should open "comparing experiment runs" page for selected experiment runs', async function() {
      this.retries(testCaseRetry);

      await driver.sleep(4000);
      await getExperimentRunElem(
        driver,
        createdExprRunsInfo.experimentRuns[0].name
      )
        .then(e => e.findElement(By.css('[data-test=comparing-toggler]')))
        .then(e => e.click());
      await driver.sleep(4000);
      await getExperimentRunElem(
        driver,
        createdExprRunsInfo.experimentRuns[1].name
      )
        .then(e => e.findElement(By.css('[data-test=comparing-toggler]')))
        .then(e => e.click());

      await driver.sleep(3000);
      const entitiesForComparing = await driver.findElements(
        By.css('[data-test=entity-for-comparing]')
      );
      await driver.sleep(3000);
      assert.equal(
        entitiesForComparing.length,
        2,
        'should display ids of experiment runs which are selected for comparing'
      );

      await driver.sleep(3000);
      await driver
        .findElement(By.css('[data-test=compare-entities-button]'))
        .click();

      await driver.sleep(3000);
      const expectedUrl = routes.projectsRoutes.makeCompareExperimentRunsRoute({
        projectId: createdExprRunsInfo.projectId,
        experimentRunId1: createdExprRunsInfo.experimentRuns[0].id,
        experimentRunId2: createdExprRunsInfo.experimentRuns[1].id,
      });
      await driver.wait(
        until.urlIs(expectedUrl),
        30000,
        'should redirect on "compare experiment runs" page'
      );
    });
  });

  testExperimentRunCommentsCRUD({
    navigateToPage: navigateToExperimentRunsPageWithWaitingLoadingEntities,
  });
});
