import { DataLocation } from 'core/shared/models/Repository/DataLocation';
import { IDatasetBlobDiff } from 'core/shared/models/Repository/Blob/DatasetBlob';

export const mockedServerAddedDatasetDiff = {
  location: ['folder1', 'folder2', 'nadf'],
  status: 'ADDED',
  dataset: {
    s3: {
      A: [
        {
          path: {
            path: 'http://zxcv.com',
            size: '1233421',
            last_modified_at_source: '53453474344',
            md5: 'string',
            sha256: 'sha',
          },
        },
        {
          path: {
            path: 'http://asdf.com',
            size: '123341',
            last_modified_at_source: '512344',
            md5: 'string',
            sha256: 'sha',
          },
        },
      ],
    },
  },
};

export const mockedClientSuccessAddedDatasetDiff: IDatasetBlobDiff = {
  diffType: 'added',
  category: 'dataset',
  type: 's3',
  location: ['folder1', 'folder2', 'nadf'] as DataLocation,
  blob: {
    category: 'dataset',
    type: 's3',
    components: [
      {
        path: {
          path: 'http://zxcv.com',
          size: 1233421,
          lastModifiedAtSource: new Date(53453474344),
          md5: 'string',
          sha256: 'sha',
        },
      },
      {
        path: {
          path: 'http://asdf.com',
          size: 123341,
          lastModifiedAtSource: new Date(512344),
          md5: 'string',
          sha256: 'sha',
        },
      },
    ],
  },
};

export const mockedServerDeletedDatasetDiff = {
  location: ['folder1', 'folder2', 'nadfgv'],
  status: 'DELETED',
  dataset: {
    path: {
      A: [
        {
          path: 'http://zxcv.com',
          size: '1233421',
          last_modified_at_source: '53453474344',
          md5: 'string',
          sha256: 'sha',
        },
        {
          path: 'http://asdf.com',
          size: '123341',
          last_modified_at_source: '512344',
          md5: 'string',
          sha256: 'sha',
        },
      ],
    },
  },
};

export const mockedClientSuccessDeletedDatasetDiff: IDatasetBlobDiff = {
  diffType: 'deleted',
  category: 'dataset',
  type: 'path',
  location: ['folder1', 'folder2', 'nadfgv'] as DataLocation,
  blob: {
    category: 'dataset',
    type: 'path',
    components: [
      {
        path: 'http://zxcv.com',
        size: 1233421,
        lastModifiedAtSource: new Date(53453474344),
        md5: 'string',
        sha256: 'sha',
      },
      {
        path: 'http://asdf.com',
        size: 123341,
        lastModifiedAtSource: new Date(512344),
        md5: 'string',
        sha256: 'sha',
      },
    ],
  },
};

export const mockedServerUpdatedDatasetDiff = {
  location: ['folder1', 'folder2', 'blob33'],
  status: 'MODIFIED',
  dataset: {
    s3: {
      A: [
        {
          path: {
            path: 'http://location2.com',
            size: '1233413',
            last_modified_at_source: '512344',
            md5: 'string',
            sha256: 'sha',
          },
        },
        {
          path: {
            path: 'http://locatiodn.com',
            size: '1233421',
            last_modified_at_source: '53453474344',
            md5: 'string',
            sha256: 'sha',
          },
        },
      ],
      B: [
        {
          path: {
            path: 'http://location2.com',
            size: '123341',
            last_modified_at_source: '512344',
            md5: 'string',
            sha256: 'sha',
          },
        },
        {
          path: {
            path: 'http://locatiodn.com',
            size: '1233421',
            last_modified_at_source: '53453474344',
            md5: 'string',
            sha256: 'sha',
          },
        },
      ],
    },
  },
};

export const mockedClientSuccessUpdatedDatasetDiff: IDatasetBlobDiff = {
  location: ['folder1', 'folder2', 'blob33'] as DataLocation,
  diffType: 'updated',
  category: 'dataset',
  type: 's3',
  blobA: {
    category: 'dataset',
    type: 's3',
    components: [
      {
        path: {
          path: 'http://location2.com',
          size: 1233413,
          lastModifiedAtSource: new Date(512344),
          md5: 'string',
          sha256: 'sha',
        },
      },
      {
        path: {
          path: 'http://locatiodn.com',
          size: 1233421,
          lastModifiedAtSource: new Date(53453474344),
          md5: 'string',
          sha256: 'sha',
        },
      },
    ],
  },
  blobB: {
    category: 'dataset',
    type: 's3',
    components: [
      {
        path: {
          path: 'http://location2.com',
          size: 123341,
          lastModifiedAtSource: new Date(512344),
          md5: 'string',
          sha256: 'sha',
        },
      },
      {
        path: {
          path: 'http://locatiodn.com',
          size: 1233421,
          lastModifiedAtSource: new Date(53453474344),
          md5: 'string',
          sha256: 'sha',
        },
      },
    ],
  },
};
