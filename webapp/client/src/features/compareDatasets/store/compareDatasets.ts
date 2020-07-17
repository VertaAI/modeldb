import * as R from 'ramda';

import { IAttribute } from 'shared/models/Attribute';
import {
    getKeyValuePairsDiff,
} from 'shared/utils/collection';
import {
    IDatasetVersion,
    IRawDatasetVersion,
    IQueryDatasetVersion,
    IPathBasedDatasetVersion,
    IDatasetPathPartInfo,
} from 'shared/models/DatasetVersion';

export enum DatasetVerisonEntityType {
    entity1 = 'entity1',
    entity2 = 'entity2',
}

export type IAttributesDiffInfo = Record<
    string,
    | { type: 'singleAttribute' }
    | { type: 'singleValueTypes'; isDifferent: boolean }
    | { type: 'differentValueTypes' }
    | { type: 'listValueTypes'; diffInfo: { [value: string]: boolean } }
>;

export type ComparedDatasetVersions = [IDatasetVersion?, IDatasetVersion?];

export interface IDatasetVersionCommonDifferentProps {
    id: boolean;
    parentId: boolean;
    attributes: IAttributesDiffInfo;
    version: boolean;
    tags: boolean;
    dateLogged: boolean;
}
export type IRawDatasetVersionDifferentProps = {
    size: boolean;
    features: {
        [key: string]: boolean;
    };
    numRecords: boolean;
    objectPath: boolean;
    checkSum: boolean;
} & IDatasetVersionCommonDifferentProps;

export type IQueryDatasetVersionDifferentProps = {
    query: boolean;
    queryTemplate: boolean;
    queryParameters: {
        [key: string]: boolean;
    };
    dataSourceUri: boolean;
    executionTimestamp: boolean;
    numRecords: boolean;
} & IDatasetVersionCommonDifferentProps;

export type IPathDatasetVersionDifferentProps = {
    size: boolean;
    basePath: boolean;
    locationType: boolean;
    datasetPathInfos: IDiffDatasetPathInfos;
} & IDatasetVersionCommonDifferentProps;
export type IDiffDatasetPathInfos = Record<
    string,
    { path: boolean; size: boolean; checkSum: boolean; lastModified: boolean }
>;
export type IDatasetVersionsDifferentProps =
    | IRawDatasetVersionDifferentProps
    | IQueryDatasetVersionDifferentProps
    | IPathDatasetVersionDifferentProps;

export const getDatasetVersionsDifferentProps = (
    datasetVersion1: IDatasetVersion,
    datasetVersion2: IDatasetVersion
): IDatasetVersionsDifferentProps => {
    if (datasetVersion1.type === 'raw' && datasetVersion2.type === 'raw') {
        return getRawDatasetVersionsDifferentProps(
            datasetVersion1,
            datasetVersion2
        );
    }
    if (datasetVersion1.type === 'query' && datasetVersion2.type === 'query') {
        return getQueryDatasetVersionsDifferentProps(
            datasetVersion1,
            datasetVersion2
        );
    }
    if (datasetVersion1.type === 'path' && datasetVersion2.type === 'path') {
        return getPathDatasetVersionsDifferentProps(
            datasetVersion1,
            datasetVersion2
        );
    }
    return {} as any;
};
const getDatasetVersionCommonDifferentProps = (
    datasetVersion1: IDatasetVersion,
    datasetVersion2: IDatasetVersion
): IDatasetVersionCommonDifferentProps => {
    return {
        id: datasetVersion1.id !== datasetVersion2.id,
        parentId: datasetVersion1.parentId !== datasetVersion2.parentId,
        tags: !R.equals(datasetVersion1.tags, datasetVersion2.tags),
        version: datasetVersion1.version !== datasetVersion2.version,
        attributes: compareAttributes(
            datasetVersion1.attributes,
            datasetVersion2.attributes
        ),
        dateLogged: datasetVersion1.dateLogged !== datasetVersion2.dateLogged,
    };
};
const getRawDatasetVersionsDifferentProps = (
    datasetVersion1: IRawDatasetVersion,
    datasetVersion2: IRawDatasetVersion
): IRawDatasetVersionDifferentProps => {
    return {
        ...getDatasetVersionCommonDifferentProps(datasetVersion1, datasetVersion2),
        size: datasetVersion1.info.size !== datasetVersion2.info.size,
        features: getKeyValuePairsDiff(
            datasetVersion1.info.features.map(feature => ({
                key: feature,
                value: feature,
            })),
            datasetVersion2.info.features.map(feature => ({
                key: feature,
                value: feature,
            }))
        ),
        numRecords:
            datasetVersion1.info.numRecords !== datasetVersion2.info.numRecords,
        objectPath:
            datasetVersion1.info.objectPath !== datasetVersion2.info.objectPath,
        checkSum: datasetVersion1.info.checkSum !== datasetVersion2.info.checkSum,
    };
};
const getQueryDatasetVersionsDifferentProps = (
    datasetVersion1: IQueryDatasetVersion,
    datasetVersion2: IQueryDatasetVersion
): IQueryDatasetVersionDifferentProps => {
    return {
        ...getDatasetVersionCommonDifferentProps(datasetVersion1, datasetVersion2),
        query: datasetVersion1.info.query !== datasetVersion2.info.query,
        queryParameters: getKeyValuePairsDiff(
            datasetVersion1.info.queryParameters.map(({ name, value }) => ({
                key: name,
                value,
            })),
            datasetVersion2.info.queryParameters.map(({ name, value }) => ({
                key: name,
                value,
            }))
        ),
        queryTemplate:
            datasetVersion1.info.queryTemplate !== datasetVersion2.info.queryTemplate,
        dataSourceUri:
            datasetVersion1.info.dataSourceUri !== datasetVersion2.info.dataSourceUri,
        executionTimestamp:
            datasetVersion1.info.executionTimestamp !==
            datasetVersion2.info.executionTimestamp,
        numRecords:
            datasetVersion1.info.numRecords !== datasetVersion2.info.numRecords,
    };
};
const getPathDatasetVersionsDifferentProps = (
    datasetVersion1: IPathBasedDatasetVersion,
    datasetVersion2: IPathBasedDatasetVersion
): IPathDatasetVersionDifferentProps => {
    return {
        ...getDatasetVersionCommonDifferentProps(datasetVersion1, datasetVersion2),
        basePath: datasetVersion1.info.basePath !== datasetVersion2.info.basePath,
        locationType:
            datasetVersion1.info.locationType !== datasetVersion2.info.locationType,
        size: datasetVersion1.info.size !== datasetVersion2.info.size,
        datasetPathInfos: compareDatasetPathInfos(
            datasetVersion1.info.datasetPathInfos,
            datasetVersion2.info.datasetPathInfos
        ),
    };
};
const compareDatasetPathInfos = (
    datasetPathInfos1: IDatasetPathPartInfo[],
    datasetPathInfos2: IDatasetPathPartInfo[]
): IDiffDatasetPathInfos => {
    const paths = R.uniq(
        datasetPathInfos1
            .map(({ path }) => path)
            .concat(datasetPathInfos2.map(({ path }) => path))
    );
    return R.fromPairs(
        paths.map(path => {
            const pathInfo1 = datasetPathInfos1.find(x => x.path === path);
            const pathInfo2 = datasetPathInfos2.find(x => x.path === path);
            if ((!pathInfo1 && pathInfo2) || (pathInfo1 && !pathInfo2)) {
                return [
                    path,
                    { path: true, size: true, checkSum: true, lastModified: true },
                ];
            }
            return [
                path,
                {
                    path: false,
                    size: pathInfo1!.size !== pathInfo2!.size,
                    checkSum: pathInfo1!.checkSum !== pathInfo2!.checkSum,
                    lastModified: pathInfo1!.lastModified !== pathInfo2!.lastModified,
                },
            ];
        })
    ) as any;
};

const compareAttributes = (
    attributes1: IAttribute[],
    attributes2: IAttribute[]
): IAttributesDiffInfo => {
    const attributesKeys = R.uniq(
        attributes1.map(({ key }) => key).concat(attributes2.map(({ key }) => key))
    );
    return R.fromPairs(
        attributesKeys.map(attributeKey => {
            const attribute1 = attributes1.find(({ key }) => key === attributeKey);
            const attribute2 = attributes2.find(({ key }) => key === attributeKey);
            if ((attribute1 && !attribute2) || (!attribute1 && attribute2)) {
                return [attributeKey, { type: 'singleAttribute' }];
            }
            if (
                Array.isArray(attribute1!.value) &&
                Array.isArray(attribute2!.value)
            ) {
                return [
                    attributeKey,
                    {
                        type: 'listValueTypes',
                        diffInfo: getKeyValuePairsDiff(
                            attribute1!.value.map(x => ({
                                key: x,
                                value: x,
                            })),
                            attribute2!.value.map(x => ({
                                key: x,
                                value: x,
                            }))
                        ),
                    },
                ];
            }
            if ([attribute1!.value, attribute2!.value].some(Array.isArray)) {
                return [attributeKey, { type: 'differentValueTypes' }];
            }
            return [
                attributeKey,
                {
                    type: 'singleValueTypes',
                    isDifferent: attribute1!.value !== attribute2!.value,
                },
            ];
        })
    ) as any;
};
