import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import {
  IDatasetBlobDiff,
  IPathDatasetComponentBlob,
  IPathDatasetBlobDiff,
  IPathDatasetBlobDiffData,
} from 'core/shared/models/Versioning/Blob/DatasetBlob';

import DatasetDiffView from '../DatasetDiffView';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';
import { DataLocation } from 'core/shared/models/Versioning/DataLocation';
import {
  DiffType,
  elementDiffMakers,
} from 'core/shared/models/Versioning/Blob/Diff';

const makeComponent = ({ diff }: { diff: IDatasetBlobDiff }) => {
  return mount(<DatasetDiffView diff={diff} />);
};

const findDisplayedPaths = (component: ReactWrapper) => {
  const getDiffColor = (elem: ReactWrapper) => {
    if (((elem.prop('className') || '') as string).includes('redDiff')) {
      return 'red';
    } else if (
      ((elem.prop('className') || '') as string).includes('greenDiff')
    ) {
      return 'green';
    }
    return undefined;
  };

  return component
    .find('tr')
    .slice(1)
    .map(pathRow => {
      return {
        path: {
          text: findByDataTestAttribute('path', pathRow).text(),
          diffColor: getDiffColor(findByDataTestAttribute('path', pathRow)),
        },
        md5: {
          text: findByDataTestAttribute('md5', pathRow).text(),
          diffColor: getDiffColor(findByDataTestAttribute('md5', pathRow)),
        },
      };
    });
};

const pathDatasetComponents: IPathDatasetComponentBlob[] = [
  {
    path: 'http://aadfadflocation2.comadf',
    size: 113413423341,
    lastModifiedAtSource: new Date(),
    md5: 'stradfadfing',
    sha256: '',
  },
  {
    path: 'http://locatieeeeeeeeon2.comadf',
    size: 123341,
    lastModifiedAtSource: new Date(),
    md5: 'string',
    sha256: '',
  },
];

const makePathDatasetBlobDiff = ({
  diffType,
  components,
}: {
  diffType: DiffType;
  components: IPathDatasetBlobDiffData['components'];
}): IPathDatasetBlobDiff => {
  return {
    category: 'dataset',
    diffType,
    location: ['blob33'] as DataLocation,
    data: {
      category: 'dataset',
      type: 'path',
      components,
    },
    type: 'path',
  };
};

describe('(compareCommits feature) DatasetDiffView', () => {
  it('should display paths with green highlithing when diff status is added', () => {
    const diff = makePathDatasetBlobDiff({
      diffType: 'added',
      components: [
        elementDiffMakers.added(pathDatasetComponents[0]),
        elementDiffMakers.added(pathDatasetComponents[1]),
      ],
    });
    const component = makeComponent({ diff });

    expect(findDisplayedPaths(component)).toEqual([
      {
        path: { text: pathDatasetComponents[0].path, diffColor: 'green' },
        md5: { text: pathDatasetComponents[0].md5, diffColor: 'green' },
      },
      {
        path: { text: pathDatasetComponents[1].path, diffColor: 'green' },
        md5: { text: pathDatasetComponents[1].md5, diffColor: 'green' },
      },
    ]);
  });

  it('should display paths with red highlithing when diff status is deleted', () => {
    const diff: IDatasetBlobDiff = makePathDatasetBlobDiff({
      diffType: 'deleted',
      components: [
        elementDiffMakers.deleted(pathDatasetComponents[0]),
        elementDiffMakers.deleted(pathDatasetComponents[1]),
      ],
    });

    const component = makeComponent({ diff });

    expect(findDisplayedPaths(component)).toEqual([
      {
        path: { text: pathDatasetComponents[0].path, diffColor: 'red' },
        md5: { text: pathDatasetComponents[0].md5, diffColor: 'red' },
      },
      {
        path: { text: pathDatasetComponents[1].path, diffColor: 'red' },
        md5: { text: pathDatasetComponents[1].md5, diffColor: 'red' },
      },
    ]);
  });

  describe('when modified status', () => {
    it('should display deleted paths with red highlithing', () => {
      const diff: IDatasetBlobDiff = makePathDatasetBlobDiff({
        diffType: 'modified',
        components: [
          elementDiffMakers.deleted(pathDatasetComponents[0]),
          elementDiffMakers.deleted(pathDatasetComponents[1]),
        ],
      });

      const component = makeComponent({ diff });

      expect(findDisplayedPaths(component)).toEqual([
        {
          path: { text: pathDatasetComponents[0].path, diffColor: 'red' },
          md5: { text: pathDatasetComponents[0].md5, diffColor: 'red' },
        },
        {
          path: { text: pathDatasetComponents[1].path, diffColor: 'red' },
          md5: { text: pathDatasetComponents[1].md5, diffColor: 'red' },
        },
      ]);
    });

    it('should display added paths with green highlithing', () => {
      const diff: IDatasetBlobDiff = makePathDatasetBlobDiff({
        diffType: 'modified',
        components: [
          elementDiffMakers.added(pathDatasetComponents[0]),
          elementDiffMakers.added(pathDatasetComponents[1]),
        ],
      });

      const component = makeComponent({ diff });

      expect(findDisplayedPaths(component)).toEqual([
        {
          path: { text: pathDatasetComponents[0].path, diffColor: 'green' },
          md5: { text: pathDatasetComponents[0].md5, diffColor: 'green' },
        },
        {
          path: { text: pathDatasetComponents[1].path, diffColor: 'green' },
          md5: { text: pathDatasetComponents[1].md5, diffColor: 'green' },
        },
      ]);
    });

    it('should display B and A of modified paths and highlight changed properties with green for B and red for A', () => {
      const A = {
        ...pathDatasetComponents[0],
        path: 'http://aa.com',
        md5: 'qwer',
      };
      const B = {
        ...pathDatasetComponents[0],
        path: 'http://aa.com',
        md5: 'qweradfadf',
      };
      const diff: IDatasetBlobDiff = makePathDatasetBlobDiff({
        diffType: 'modified',
        components: [elementDiffMakers.modified(A, B)],
      });

      const component = makeComponent({ diff });

      expect(findDisplayedPaths(component)).toEqual([
        {
          path: { text: A.path, diffColor: undefined },
          md5: { text: A.md5, diffColor: 'red' },
        },
        {
          path: { text: B.path, diffColor: undefined },
          md5: { text: B.md5, diffColor: 'green' },
        },
      ]);
    });

    it('should display diff for components without changing order', () => {
      const A = {
        ...pathDatasetComponents[0],
        path: 'http://aa.com',
        md5: 'qwer',
      };
      const B = {
        ...pathDatasetComponents[0],
        path: 'http://aa.com',
        md5: 'qweradfadf',
      };
      const diff: IDatasetBlobDiff = makePathDatasetBlobDiff({
        diffType: 'modified',
        components: [
          elementDiffMakers.deleted(pathDatasetComponents[0]),
          elementDiffMakers.modified(A, B),
        ],
      });

      const component = makeComponent({ diff });

      expect(findDisplayedPaths(component)).toEqual([
        {
          path: { text: pathDatasetComponents[0].path, diffColor: 'red' },
          md5: { text: pathDatasetComponents[0].md5, diffColor: 'red' },
        },
        {
          path: { text: A.path, diffColor: undefined },
          md5: { text: A.md5, diffColor: 'red' },
        },
        {
          path: { text: B.path, diffColor: undefined },
          md5: { text: B.md5, diffColor: 'green' },
        },
      ]);
    });
  });
});
