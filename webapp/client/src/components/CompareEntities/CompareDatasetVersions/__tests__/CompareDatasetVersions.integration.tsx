import { ReactWrapper, ShallowWrapper } from 'enzyme';
import { Omit } from 'ramda';
import * as React from 'react';
import { action } from 'typesafe-actions';

import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { formatBytes } from 'core/shared/utils/mapperConverters';
import flushAllPromises from 'core/shared/utils/tests/integrations/flushAllPromises';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';
import { Dataset } from 'models/Dataset';
import {
  IDatasetVersion,
  IRawDatasetVersion,
  IQueryDatasetVersion,
  IPathBasedDatasetVersion,
} from 'models/DatasetVersion';
import DatasetVersionsDataService from 'services/datasetVersions/DatasetVersionsDataService';
import {
  IDatasetsState,
  datasetsReducer,
  loadDatasetActionTypes,
} from 'store/datasets';
import makeMountComponentWithPredefinedData from 'utils/tests/integrations/makeMountComponentWithPredefinedData';
import { makeDataset } from 'utils/tests/mocks/models/datasetMocks';

import diffHighlightStyles from '../../shared/DiffHighlight/DiffHighlight.module.css';
import CompareDatasetVersions, {
  ICompareDatasetVersionsLocalProps,
} from '../CompareDatasetVersions';
import CompareDatasetVersionsTable from '../CompareDatasetVersionsTable/CompareDatasetVersionsTable';

jest.mock('services/datasetVersions/DatasetVersionsDataService');

const getDatasetsState = (dataset: Dataset): IDatasetsState => {
  return datasetsReducer(
    undefined,
    action(loadDatasetActionTypes.SUCCESS, { dataset })
  );
};

const mockDataset = makeDataset({
  id: 'id',
  name: 'dataset',
  attributes: [],
  description: 'description',
  type: 'raw',
});

const renderComponent = async ({
  datasetVersion1,
  datasetVersion2,
}: {
  datasetVersion1: IDatasetVersion;
  datasetVersion2: IDatasetVersion;
}) => {
  const defaultProps: ICompareDatasetVersionsLocalProps = {
    comparedDatasetVersionIds: [datasetVersion1.id, datasetVersion2.id],
    datasetId: mockDataset.id,
  };

  (DatasetVersionsDataService as any).mockImplementation(() => ({
    loadDatasetVersion: jest.fn(id =>
      Promise.resolve(
        datasetVersion1.id === id ? datasetVersion1 : datasetVersion2
      )
    ),
  }));

  const integratingTestData = await makeMountComponentWithPredefinedData({
    Component: () => <CompareDatasetVersions {...defaultProps} />,
    settings: {
      initialState: {
        datasets: getDatasetsState({
          ...mockDataset,
          type: datasetVersion1.type,
        }),
      },
    },
  });

  await flushAllPromises();
  await flushAllPromises();

  integratingTestData.component.update();

  return integratingTestData;
};

const checkBgHighlighting = (
  modelType: 'first' | 'second',
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return component
    .getDOMNode()
    .classList.contains(
      diffHighlightStyles[
        `highlightBg_${modelType === 'first' ? 'entity1' : 'entity2'}`
      ]
    );
};

const checkBorderHighlighting = (
  modelType: 'first' | 'second',
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return component
    .getDOMNode()
    .classList.contains(
      diffHighlightStyles[
        `highlightBorder_${modelType === 'first' ? 'entity1' : 'entity2'}`
      ]
    );
};

const findPropertyRowNameElem = (
  prop: string,
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return findByDataTestAttribute(`property-name-${prop}`, component);
};
const findPropertyValueElem = (
  prop: string,
  type: 'first' | 'second',
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return findByDataTestAttribute(`property-value-${prop}`, component).at(
    type === 'first' ? 0 : 1
  );
};
const findPropertyValueText = (
  prop: string,
  type: 'first' | 'second',
  component: ReactWrapper<any, any> | ShallowWrapper<any, any>
) => {
  return findPropertyValueElem(prop, type, component).text();
};

const testRenderPropertyRow = (
  propertyInfo: { propertyType: string; propertyTitle: string },
  componentData: {
    datasetVersion1: IDatasetVersion;
    datasetVersion2: IDatasetVersion;
  }
) => {
  it('should render comparing row for property', async () => {
    const { component } = await renderComponent(componentData);

    expect(
      findPropertyRowNameElem(propertyInfo.propertyType, component).length
    ).toBe(1);
    expect(
      findPropertyRowNameElem(propertyInfo.propertyType, component).text()
    ).toBe(propertyInfo.propertyTitle);
  });
};

const testRenderPropertyValue = <T extends IDatasetVersion>(
  propertyInfo: {
    propertyType: string;
    getExpectedValue: (datasetVersion1: T) => any;
  },
  componentData: { datasetVersion1: T; datasetVersion2: T }
) => {
  it('should render correct value for the first dataset version', async () => {
    const { component } = await renderComponent(componentData);

    expect(
      findPropertyValueText(propertyInfo.propertyType, 'first', component)
    ).toBe(propertyInfo.getExpectedValue(componentData.datasetVersion1));
  });
};

const testBgHighlightingDiffValue = (
  propertyInfo: { propertyType: string; isDiff: boolean },
  componentData: {
    datasetVersion1: IDatasetVersion;
    datasetVersion2: IDatasetVersion;
  }
) => {
  const message = propertyInfo.isDiff
    ? 'should highlight background values if they are not equal'
    : 'should not highlight background values if they are equal';
  it(message, async () => {
    const { component } = await renderComponent(componentData);
    const firstPropElem = findPropertyValueElem(
      propertyInfo.propertyType,
      'first',
      component
    );
    const secondPropElem = findPropertyValueElem(
      propertyInfo.propertyType,
      'second',
      component
    );

    expect(checkBgHighlighting('first', firstPropElem)).toBe(
      propertyInfo.isDiff
    );
    expect(checkBgHighlighting('second', secondPropElem)).toBe(
      propertyInfo.isDiff
    );
  });
};

const testDisplayingNoDataStubWhenDataIsNotExisted = (
  propertyInfo: { propertyType: string },
  componentData: {
    datasetVersion1: IDatasetVersion;
    datasetVersion2: IDatasetVersion;
  }
) => {
  it('should display "-" if property is not existed', async () => {
    const { component } = await renderComponent(componentData);

    expect(
      findPropertyValueText(propertyInfo.propertyType, 'first', component)
    ).toBe('-');
    expect(
      findPropertyValueText(propertyInfo.propertyType, 'second', component)
    ).toBe('-');
  });
};

describe('component', () => {
  describe('CompareDatasetVersions', () => {
    const datasetVersion1: IRawDatasetVersion = {
      ...mockDataset,
      id: 'datasetVersion-1',
      type: 'raw',
      datasetId: mockDataset.id,
      attributes: [],
      dateLogged: new Date(),
      dateUpdated: new Date(),
      description: 'description',
      version: 0,
      info: {
        features: [],
      },
    };

    const datasetVersion2: IRawDatasetVersion = {
      ...mockDataset,
      id: 'datasetVersion-2',
      type: 'raw',
      datasetId: mockDataset.id,
      attributes: [],
      dateLogged: new Date(),
      dateUpdated: new Date(),
      description: 'description',
      version: 0,
      info: {
        features: [],
      },
    };

    it('should display table with comparing dataset versions info if dataset versions are loaded', async () => {
      const { component } = await renderComponent({
        datasetVersion1,
        datasetVersion2,
      });

      expect(
        findByDataTestAttribute('compare-dataset-versions-prealoder', component)
          .length
      ).toBe(0);
      expect(component.find(CompareDatasetVersionsTable).length).toBe(1);
    });

    describe('when dataset versions type is raw', () => {
      const updateRawDatasetVersion = (
        newProps: Partial<Omit<IRawDatasetVersion, 'info'>> & {
          info?: Partial<IRawDatasetVersion['info']>;
        },
        rawDatasetVersion: IRawDatasetVersion
      ): IRawDatasetVersion => {
        const updatedData = {
          ...rawDatasetVersion,
          ...newProps,
        };
        if (!updatedData.info.features) {
          return {
            ...updatedData,
            info: {
              ...updatedData.info,
              features: [],
            },
          } as IRawDatasetVersion;
        }
        return updatedData as IRawDatasetVersion;
      };

      const rawDatasetVersion1: IRawDatasetVersion = {
        ...mockDataset,
        id: 'datasetVersion-1',
        type: 'raw',
        datasetId: mockDataset.id,
        attributes: [],
        dateLogged: new Date(),
        dateUpdated: new Date(),
        description: 'description',
        version: 0,
        info: {
          features: [],
        },
      };

      const rawDatasetVersion2: IRawDatasetVersion = {
        ...mockDataset,
        id: 'datasetVersion-2',
        type: 'raw',
        datasetId: mockDataset.id,
        attributes: [],
        dateLogged: new Date(),
        dateUpdated: new Date(),
        description: 'description',
        version: 0,
        info: {
          features: [],
        },
      };

      describe('property "id"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'id',
            propertyTitle: 'Id',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'id',
            getExpectedValue: ({ id }) => id,
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testBgHighlightingDiffValue(
          {
            isDiff: true,
            propertyType: 'id',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );
      });

      describe('property "parent id"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'parentId',
            propertyTitle: 'Parent Id',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'parentId',
            getExpectedValue: ({ parentId }) => parentId,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { parentId: 'parentId' },
              rawDatasetVersion1
            ),
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testBgHighlightingDiffValue(
          {
            isDiff: true,
            propertyType: 'parentId',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { parentId: 'parentId-1' },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { parentId: 'parentId-2' },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            isDiff: false,
            propertyType: 'parentId',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { parentId: 'parentId' },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { parentId: 'parentId' },
              rawDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'parentId',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { parentId: undefined },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { parentId: undefined },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "version"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'version',
            propertyTitle: 'Version',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'version',
            getExpectedValue: _ => '1',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { version: 1 },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { version: 1 },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            isDiff: true,
            propertyType: 'version',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { version: 1 },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { version: 2 },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            isDiff: false,
            propertyType: 'version',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { version: 1 },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { version: 1 },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "Attribute"', () => {
        const getDisplayedAttributes = (
          modelType: 'first' | 'second',
          component: ReactWrapper<any, any>
        ) => {
          return component
            .find('[data-test="attributes"]')
            .at(modelType === 'first' ? 0 : 1)
            .find('[data-test="attribute"]')
            .map(attributeElem => ({
              button: attributeElem,
              key: {
                data: attributeElem.find('[data-test="attribute-key"]').text(),
                elem: attributeElem.find('[data-test="attribute-key"]'),
              },
            }));
        };

        testRenderPropertyRow(
          {
            propertyType: 'attributes',
            propertyTitle: 'Attributes',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        it('should highlight border of attribute button if attributes with the same key have not the same value or if attribute with key is not existed', async () => {
          const datasetVersion1Attributes = [
            { key: 'attribute-1', value: 0.000001 },
            { key: 'attribute-2', value: 'lbfgs' },
            { key: 'attribute-3', value: 15 },
          ];
          const datasetVersion2Attributes = [
            { key: 'attribute-1', value: 0.2 },
            { key: 'attribute-2', value: 'lbfgs' },
          ];
          const expectedResult = [
            { key: 'attribute-1', isDiff: true },
            { key: 'attribute-2', isDiff: false },
            { key: 'attribute-3', isDiff: true },
          ];
          const { component } = await renderComponent({
            datasetVersion1: updateRawDatasetVersion(
              { attributes: datasetVersion1Attributes },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { attributes: datasetVersion2Attributes },
              rawDatasetVersion2
            ),
          });

          const displayedAttributes1 = getDisplayedAttributes(
            'first',
            component
          );
          const displayedAttributes2 = getDisplayedAttributes(
            'second',
            component
          );
          expectedResult.forEach(({ key, isDiff }) => {
            const targetDisplayedAttributes1 = displayedAttributes1.find(
              h => h.key.data === key
            );
            const targetDisplayedAttributes2 = displayedAttributes2.find(
              h => h.key.data === key
            );
            if (targetDisplayedAttributes1) {
              expect(
                checkBorderHighlighting(
                  'first',
                  targetDisplayedAttributes1.button
                )
              ).toBe(isDiff);
            }
            if (targetDisplayedAttributes2) {
              expect(
                checkBorderHighlighting(
                  'second',
                  targetDisplayedAttributes2.button
                )
              ).toBe(isDiff);
            }
          });
        });

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'attributes',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { attributes: [] },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { attributes: [] },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "dateLogged"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'dateLogged',
            propertyTitle: 'Timestamp',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'dateLogged',
            getExpectedValue: ({ dateLogged }) =>
              getFormattedDateTime(dateLogged!),
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { dateLogged: new Date() },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { dateLogged: new Date() },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'dateLogged',
            isDiff: true,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { dateLogged: new Date(2000) },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { dateLogged: new Date(5000) },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'dateLogged',
            isDiff: false,
          },
          (() => {
            const dateLogged = new Date();
            return {
              datasetVersion1: updateRawDatasetVersion(
                { dateLogged },
                rawDatasetVersion1
              ),
              datasetVersion2: updateRawDatasetVersion(
                { dateLogged },
                rawDatasetVersion2
              ),
            };
          })()
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'dateLogged',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { dateLogged: undefined },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { dateLogged: undefined },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "numRecords"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'numRecords',
            propertyTitle: 'Records count',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'numRecords',
            getExpectedValue: _ => '1',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { numRecords: 1 } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { numRecords: 1 } },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'numRecords',
            isDiff: true,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { numRecords: 1 } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { numRecords: 2 } },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'numRecords',
            isDiff: false,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { numRecords: 1 } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { numRecords: 1 } },
              rawDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'numRecords',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { numRecords: undefined } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { numRecords: undefined } },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "size"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'size',
            propertyTitle: 'Size',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'size',
            getExpectedValue: ({ info: { size } }) => formatBytes(size!),
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { size: 1 } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { size: 1 } },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'size',
            isDiff: true,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { size: 1 } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { size: 2 } },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'size',
            isDiff: false,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { size: 1 } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { size: 1 } },
              rawDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'size',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { size: undefined } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { size: undefined } },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "objectPath"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'objectPath',
            propertyTitle: 'Object path',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'objectPath',
            getExpectedValue: ({ info: { objectPath } }) => objectPath,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              {
                info: {
                  objectPath:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              {
                info: {
                  objectPath:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'objectPath',
            isDiff: true,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              {
                info: {
                  objectPath:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { objectPath: 'fkadl13413i4odfdglsfk13ok1o3dkad;fk' } },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'objectPath',
            isDiff: false,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              {
                info: {
                  objectPath:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              {
                info: {
                  objectPath:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'objectPath',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { size: undefined } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { size: undefined } },
              rawDatasetVersion2
            ),
          }
        );
      });

      describe('property "checkSum"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'checkSum',
            propertyTitle: 'Checksum',
          },
          {
            datasetVersion1: rawDatasetVersion1,
            datasetVersion2: rawDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'checkSum',
            getExpectedValue: ({ info: { checkSum } }) => checkSum,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              {
                info: {
                  checkSum:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              {
                info: {
                  checkSum:
                    '876c5f81daa5e2896284a869d79b159909bdad0c489f6b9d4e714f4ccebf7f05',
                },
              },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'checkSum',
            isDiff: true,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { checkSum: 'adfadf31413sdgjksfgl' } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { checkSum: 'adf13rlk31fkadf' } },
              rawDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'checkSum',
            isDiff: false,
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { checkSum: 'adf13rlk31fkadf' } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { checkSum: 'adf13rlk31fkadf' } },
              rawDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'checkSum',
          },
          {
            datasetVersion1: updateRawDatasetVersion(
              { info: { size: undefined } },
              rawDatasetVersion1
            ),
            datasetVersion2: updateRawDatasetVersion(
              { info: { size: undefined } },
              rawDatasetVersion2
            ),
          }
        );
      });
    });

    describe('when dataset versions is query', () => {
      const updateQueryDatasetVersion = (
        newProps: Omit<Partial<IQueryDatasetVersion>, 'info'> & {
          info: Partial<IQueryDatasetVersion['info']>;
        },
        queryDatasetVersion: IQueryDatasetVersion
      ): IQueryDatasetVersion => {
        const updatedData = {
          ...queryDatasetVersion,
          ...newProps,
        };
        if (!updatedData.info.queryParameters) {
          return {
            ...updatedData,
            info: {
              ...updatedData.info,
              queryParameters: [],
            },
          } as IQueryDatasetVersion;
        }
        return updatedData as IQueryDatasetVersion;
      };

      const queryDatasetVersion1: IQueryDatasetVersion = {
        ...mockDataset,
        id: 'datasetVersion-1',
        type: 'query',
        datasetId: mockDataset.id,
        attributes: [],
        dateLogged: new Date(),
        dateUpdated: new Date(),
        description: 'description',
        version: 0,
        info: {
          queryParameters: [],
        },
      };

      const queryDatasetVersion2: IQueryDatasetVersion = {
        ...mockDataset,
        id: 'datasetVersion-2',
        type: 'query',
        datasetId: mockDataset.id,
        attributes: [],
        dateLogged: new Date(),
        dateUpdated: new Date(),
        description: 'description',
        version: 0,
        info: {
          queryParameters: [],
        },
      };

      describe('property "numRecords"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'numRecords',
            propertyTitle: 'Records count',
          },
          {
            datasetVersion1: queryDatasetVersion1,
            datasetVersion2: queryDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'numRecords',
            getExpectedValue: _ => '1',
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              { info: { numRecords: 1 } },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              { info: { numRecords: 1 } },
              queryDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'numRecords',
            isDiff: true,
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              { info: { numRecords: 1 } },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              { info: { numRecords: 2 } },
              queryDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'numRecords',
            isDiff: false,
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              { info: { numRecords: 1 } },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              { info: { numRecords: 1 } },
              queryDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'numRecords',
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              { info: { numRecords: undefined } },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              { info: { numRecords: undefined } },
              queryDatasetVersion2
            ),
          }
        );
      });

      describe('property "query"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'query',
            propertyTitle: 'Query',
          },
          {
            datasetVersion1: queryDatasetVersion1,
            datasetVersion2: queryDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'query',
            getExpectedValue: ({ info: { query } }) => query,
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              {
                info: {
                  query: `select full_name, superpower, location from superheros where first_name = 'Kirill' and last_name = 'Gulyaev`,
                },
              },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              {
                info: {
                  query: `select full_name, superpower, location from superheros where first_name = 'Kirill' and last_name = 'Gulyaev`,
                },
              },
              queryDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'query',
            isDiff: true,
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              {
                info: {
                  query: `select full_name, superpower, location from superheros where first_name = 'Kirill' and last_name = 'Gulyaev`,
                },
              },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              {
                info: {
                  query: `select location from superheros where first_name = 'Santosh' and last_name = 'Gulyaev`,
                },
              },
              queryDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'query',
            isDiff: false,
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              {
                info: {
                  query: `select full_name, superpower, location from superheros where first_name = 'Kirill' and last_name = 'Gulyaev`,
                },
              },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              {
                info: {
                  query: `select full_name, superpower, location from superheros where first_name = 'Kirill' and last_name = 'Gulyaev`,
                },
              },
              queryDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'query',
          },
          {
            datasetVersion1: updateQueryDatasetVersion(
              { info: { query: undefined } },
              queryDatasetVersion1
            ),
            datasetVersion2: updateQueryDatasetVersion(
              { info: { query: undefined } },
              queryDatasetVersion2
            ),
          }
        );
      });
    });

    describe('when dataset versions type is path', () => {
      const updatePathDatasetVersion = (
        newProps: Omit<Partial<IPathBasedDatasetVersion>, 'info'> & {
          info: Partial<IPathBasedDatasetVersion['info']>;
        },
        queryDatasetVersion: IPathBasedDatasetVersion
      ): IPathBasedDatasetVersion => {
        const updatedData = {
          ...queryDatasetVersion,
          ...newProps,
        };
        if (!updatedData.info.datasetPathInfos) {
          return {
            ...updatedData,
            info: {
              ...updatedData.info,
              datasetPathInfos: [],
            },
          } as IPathBasedDatasetVersion;
        }
        return updatedData as IPathBasedDatasetVersion;
      };

      const pathDatasetVersion1: IPathBasedDatasetVersion = {
        ...mockDataset,
        id: 'datasetVersion-1',
        type: 'path',
        datasetId: mockDataset.id,
        attributes: [],
        dateLogged: new Date(),
        dateUpdated: new Date(),
        description: 'description',
        version: 0,
        info: {
          datasetPathInfos: [],
        },
      };

      const pathDatasetVersion2: IPathBasedDatasetVersion = {
        ...mockDataset,
        id: 'datasetVersion-2',
        type: 'path',
        datasetId: mockDataset.id,
        attributes: [],
        dateLogged: new Date(),
        dateUpdated: new Date(),
        description: 'description',
        version: 0,
        info: {
          datasetPathInfos: [],
        },
      };

      describe('property "size"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'size',
            propertyTitle: 'Size',
          },
          {
            datasetVersion1: pathDatasetVersion1,
            datasetVersion2: pathDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'size',
            getExpectedValue: ({ info: { size } }) => formatBytes(size!),
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { size: 1 } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { size: 1 } },
              pathDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'size',
            isDiff: true,
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { size: 1 } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { size: 2 } },
              pathDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'size',
            isDiff: false,
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { size: 1 } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { size: 1 } },
              pathDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'size',
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { size: undefined } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { size: undefined } },
              pathDatasetVersion2
            ),
          }
        );
      });

      describe('property "basePath"', () => {
        testRenderPropertyRow(
          {
            propertyType: 'basePath',
            propertyTitle: 'Base path',
          },
          {
            datasetVersion1: pathDatasetVersion1,
            datasetVersion2: pathDatasetVersion2,
          }
        );

        testRenderPropertyValue(
          {
            propertyType: 'basePath',
            getExpectedValue: ({ info: { basePath } }) => basePath,
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { basePath: 'arn:aws:s3:::verta-condacon' } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { basePath: 'arn:aws:s3:::verta-condacon' } },
              pathDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'basePath',
            isDiff: true,
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { basePath: 'arn:aws:s3:::verta-condacon' } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { basePath: 'arn:aws:s3:::unverta-condacon' } },
              pathDatasetVersion2
            ),
          }
        );

        testBgHighlightingDiffValue(
          {
            propertyType: 'basePath',
            isDiff: false,
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { basePath: 'arn:aws:s3:::verta-condacon' } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { basePath: 'arn:aws:s3:::verta-condacon' } },
              pathDatasetVersion2
            ),
          }
        );

        testDisplayingNoDataStubWhenDataIsNotExisted(
          {
            propertyType: 'basePath',
          },
          {
            datasetVersion1: updatePathDatasetVersion(
              { info: { basePath: undefined } },
              pathDatasetVersion1
            ),
            datasetVersion2: updatePathDatasetVersion(
              { info: { basePath: undefined } },
              pathDatasetVersion2
            ),
          }
        );
      });
    });
  });
});
