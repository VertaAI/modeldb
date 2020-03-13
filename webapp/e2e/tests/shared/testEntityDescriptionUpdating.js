const { By, until } = require('selenium-webdriver');
const { assert } = require('chai');

const getDriver = require('../../helpers/getDriver');
const { testSuitRetry, testCaseRetry } = require('./testRetrySettings');

const testEntityDescriptionUpdating = ({
  entityName,
  createEntities,
  navigateToPage,
}) => {
  describe(`${entityName} description updating`, function() {
    this.retries(testSuitRetry);
    let driver;

    beforeEach(async () => {
      const createEntitiesRes = await createEntities([{ name: `${entityName}-1232231` }]);

      driver = await getDriver();
      await navigateToPage(driver, createEntitiesRes);
    });

    afterEach(async () => driver.quit());

    it(`should update ${entityName} description`, async function() {
      this.retries(testCaseRetry);
      const entity = await driver.wait(
        until.elementLocated(By.css(`[data-test=${entityName}]`)),
        30000
      );

      const entityDescription = await driver.wait(
        until.elementLocated(By.css('[data-test=description]')),
        15000
      );
      assert.exists(entityDescription, `${entityName} should has description`);
      const entityDefaultDescriptionText = await driver
        .wait(
          until.elementLocated(By.css('[data-test=description-text]')),
          15000
        )
        .getText();
      assert.equal(
        entityDefaultDescriptionText,
        'Description',
        `${entityName} should have default description = "Description"`
      );

      const openDescEditorButton = await entityDescription.findElement(
        By.css('[data-test=description-open-editor-button]')
      );
      assert.exists(openDescEditorButton, 'description should has edit button');

      await openDescEditorButton.click();

      await driver.sleep(3000);
      const descriptionEditor = await driver.findElement(
        By.css('[data-test=description-editor]')
      );
      assert.exists(
        descriptionEditor,
        'should display description editor popup'
      );

      const descriptionInput = await descriptionEditor.findElement(
        By.css('[data-test=description-input]')
      );
      assert.exists(descriptionInput, 'description editor should has input');

      await driver.sleep(1000);
      await descriptionInput.sendKeys('new description');
      await descriptionEditor
        .findElement(By.css('[data-test=description-update-button]'))
        .click();

      const entityNewDescriptionTextEl = await entityDescription.findElement(
        By.css('[data-test=description-text]')
      );
      await entityDescription
        .getDriver()
        .wait(
          until.elementTextContains(
            entityNewDescriptionTextEl,
            'new description'
          ),
          30000,
          'should update description'
        );
    });
  });
};

module.exports = testEntityDescriptionUpdating;
