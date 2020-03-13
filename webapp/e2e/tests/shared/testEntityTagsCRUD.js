const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../helpers/getDriver');
const { testSuitRetry, testCaseRetry } = require('./testRetrySettings');

const testEntityTagsCRUD = ({ entityName, createEntities, navigateToPage }) => {
  describe('tags CRUD', () => {
    const createdEntityName = `${entityName}-adfkl134`;

    describe('tags creating', function() {
      this.retries(testSuitRetry);
      let driver;

      beforeEach(async () => {
        const createEntitiesRes = await createEntities([
          { name: createdEntityName, tags: [] },
        ]);

        driver = await getDriver();
        await navigateToPage(driver, createEntitiesRes);
      });

      afterEach(async () => driver.quit());

      it(`should create a tag for ${entityName}`, async function() {
        this.retries(testCaseRetry);

        const entity = await driver.wait(
          until.elementLocated(By.css(`[data-test=${entityName}]`)),
          30000
        );

        const tagsManager = await entity
          .getDriver()
          .wait(
            until.elementLocated(By.css(`[data-test=tags-manager]`)),
            30000,
            `${entityName} should has tags manager`
          );
        assert.equal(
          await tagsManager.getText(),
          'Tags',
          'should display "Tags" text if project has not tags'
        );

        const openTagsCreatorButton = await driver.wait(
          until.elementLocated(
            By.css('[data-test=tags-manager-open-tag-creator-button]')
          ),
          15000,
          'should display button for open tag creator'
        );

        await openTagsCreatorButton.click();

        await driver.sleep(3000);
        const tagCreator = await driver.findElement(
          By.css('[data-test=tags-manager-creator]')
        );
        assert.exists(tagCreator, 'should display tag creator');

        await tagCreator
          .findElement(By.css('[data-test=tags-manager-creator-input]'))
          .sendKeys('new tag');
        await tagCreator
          .findElement(By.css('[data-test=tags-manager-create-button]'))
          .click();

        await driver.sleep(5000);

        const tagsElems = await tagsManager.findElements(
          By.css('[data-test=tags-manager-tag]')
        );
        assert.equal(tagsElems.length, 1, `${entityName} should has 1 new tag`);
        assert.equal(
          await tagsElems[0].getText(),
          'new tag',
          `${entityName} should new tag with text "new tag"`
        );
      });
    });

    describe('tags deleting', function() {
      this.retries(testSuitRetry);
      let driver;

      beforeEach(async () => {
        const createEntitiesRes = await createEntities([
          { name: createdEntityName, tags: ['tag'] },
        ]);

        driver = await getDriver();
        await navigateToPage(driver, createEntitiesRes);
      });

      afterEach(async () => driver.quit());

      it(`should delete tag from ${entityName}`, async function() {
        this.retries(testCaseRetry);

        const entity = await driver.wait(
          until.elementLocated(By.css(`[data-test=${entityName}]`)),
          30000
        );

        const deleteTagButton = await driver.wait(
          until.elementLocated(
            By.css('[data-test=tags-manager-open-tag-deletion-confirm]')
          ),
          15000,
          'should display button for tag deleting'
        );
        assert.exists(deleteTagButton, 'tags should have delete button');

        await deleteTagButton.click();

        await driver.sleep(3000);
        const confirmDeletion = await driver.findElement(
          By.css('[data-test=confirm]')
        );
        assert.exists(
          confirmDeletion,
          'should show confirmation for tag deletion'
        );

        await confirmDeletion
          .findElement(By.css('[data-test=confirm-ok-button]'))
          .click();
        await driver.sleep(5000);

        const tagsElems = await entity.findElements(
          By.css('[data-test=tags-manager-tag]')
        );
        assert.equal(
          tagsElems.length,
          0,
          `${entityName} tag should be deleted`
        );
      });
    });
  });
};

module.exports = testEntityTagsCRUD;
