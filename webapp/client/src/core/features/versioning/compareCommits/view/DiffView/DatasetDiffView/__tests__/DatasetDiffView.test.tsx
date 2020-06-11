import { mount, ReactWrapper } from 'enzyme';
import * as React from 'react';

import {
  IDatasetBlobDiff,
  IPathDatasetComponentBlob,
  IPathDatasetBlobDiff,
  IPathDatasetBlobDiffData,
} from 'core/shared/models/Versioning/Blob/DatasetBlob';

import {
  DiffType,
  elementDiffMakers,
} from 'core/shared/models/Versioning/Blob/Diff';
import { CommitComponentLocation } from 'core/shared/models/Versioning/CommitComponentLocation';
import { Row } from 'core/shared/view/elements/Table/components';

import { DiffColor } from '../../../model';
import { getDiffColorFromBackgroundColor } from '../../shared/ComparePropertiesTable/__tests__/helpers';
import DatasetDiffView from '../DatasetDiffView';

const makeComponent = ({ diff }: { diff: IDatasetBlobDiff }) => {
  return mount(<DatasetDiffView diff={diff} />);
};

const findDisplayedPaths = (component: ReactWrapper) => {
  function getColumn<T>(
    type: string,
    getContent: (column: ReactWrapper) => T,
    component: ReactWrapper<React.ComponentProps<typeof Row>>
  ): { content: undefined | T; diffColor: undefined | DiffColor } {
    const column = component.find(`[data-type="${type}"]`);
    if (column.length === 0) {
      return { content: undefined, diffColor: undefined };
    }
    return {
      content: getContent(column),
      diffColor: getDiffColorFromBackgroundColor(column.prop('style')),
    };
  }

  return component.find(Row).map(pathRow => {
    return {
      path: getColumn('path', column => column.text(), pathRow),
      md5: getColumn('md5', column => column.text(), pathRow),
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
    location: ['blob33'] as CommitComponentLocation,
    data: {
      category: 'dataset',
      type: 'path',
      components,
    },
    type: 'path',
  };
};

describe('(compareCommits feature) DatasetDiffView', () => {
  it('should display paths with bDiff highlithing when diff status is added', () => {
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
        path: { content: pathDatasetComponents[0].path, diffColor: 'bDiff' },
        md5: { content: pathDatasetComponents[0].md5, diffColor: 'bDiff' },
      },
      {
        path: { content: pathDatasetComponents[1].path, diffColor: 'bDiff' },
        md5: { content: pathDatasetComponents[1].md5, diffColor: 'bDiff' },
      },
    ]);
  });

  it('should display paths with aDiff highlithing when diff status is deleted', () => {
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
        path: { content: pathDatasetComponents[0].path, diffColor: 'aDiff' },
        md5: { content: pathDatasetComponents[0].md5, diffColor: 'aDiff' },
      },
      {
        path: { content: pathDatasetComponents[1].path, diffColor: 'aDiff' },
        md5: { content: pathDatasetComponents[1].md5, diffColor: 'aDiff' },
      },
    ]);
  });

  describe('when modified status', () => {
    it('should display deleted paths with aDiff highlithing', () => {
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
          path: { content: pathDatasetComponents[0].path, diffColor: 'aDiff' },
          md5: { content: pathDatasetComponents[0].md5, diffColor: 'aDiff' },
        },
        {
          path: { content: pathDatasetComponents[1].path, diffColor: 'aDiff' },
          md5: { content: pathDatasetComponents[1].md5, diffColor: 'aDiff' },
        },
      ]);
    });

    it('should display added paths with bDiff highlithing', () => {
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
          path: { content: pathDatasetComponents[0].path, diffColor: 'bDiff' },
          md5: { content: pathDatasetComponents[0].md5, diffColor: 'bDiff' },
        },
        {
          path: { content: pathDatasetComponents[1].path, diffColor: 'bDiff' },
          md5: { content: pathDatasetComponents[1].md5, diffColor: 'bDiff' },
        },
      ]);
    });

    it('should display B and A of modified paths and highlight changed properties with bDiff for B and aDiff for A', () => {
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
          path: { content: A.path, diffColor: undefined },
          md5: { content: A.md5, diffColor: 'aDiff' },
        },
        {
          path: { content: B.path, diffColor: undefined },
          md5: { content: B.md5, diffColor: 'bDiff' },
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
          path: { content: pathDatasetComponents[0].path, diffColor: 'aDiff' },
          md5: { content: pathDatasetComponents[0].md5, diffColor: 'aDiff' },
        },
        {
          path: { content: A.path, diffColor: undefined },
          md5: { content: A.md5, diffColor: 'aDiff' },
        },
        {
          path: { content: B.path, diffColor: undefined },
          md5: { content: B.md5, diffColor: 'bDiff' },
        },
      ]);
    });
  });
});
