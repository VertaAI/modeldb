import * as DataLocation from 'core/shared/models/Versioning/DataLocation';
import {
  IFolder,
  ISubFolderElement,
  IFolderElement,
} from 'core/shared/models/Versioning/RepositoryData';
import { S3DatasetBlob } from 'core/shared/utils/tests/mocks/Versioning/blobMocks';

import * as B from '../index';

const createdByCommitSha = 'adf';

describe('commitTreeBuilder', () => {
  it('should create the root', () => {
    const root = B.root([
      B.folder('folder-deep-1', { createdByCommitSha }, [
        B.folder('folder-deep-2', { createdByCommitSha }, []),
        B.blob('blob-deep-2', { createdByCommitSha }, S3DatasetBlob),
      ]),
    ]);

    expect(root.elements['folder-deep-1'].location).toEqual(
      DataLocation.makeFromNames(['folder-deep-1' as any])
    );
    const rootFolder: IFolder = {
      type: 'folder',
      blobs: [
        {
          type: 'blob',
          createdByCommitSha,
          name: 'blob-deep-2' as any,
        },
      ],
      subFolders: [
        {
          type: 'folder',
          createdByCommitSha,
          name: 'folder-deep-2' as any,
        },
      ],
    };
    expect(root.elements['folder-deep-1'].asDataElement).toEqual(rootFolder);
    const folderElement: ISubFolderElement = {
      createdByCommitSha,
      name: 'folder-deep-1' as any,
      type: 'folder',
    };
    expect(root.elements['folder-deep-1'].asFolderElement).toEqual(
      folderElement
    );
    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2']
    ).toBeTruthy();
    expect(root.elements['folder-deep-1'].elements['blob-deep-2']).toBeTruthy();
  });

  it('should create elements on the 2 level deep', () => {
    const root = B.root([
      B.folder('folder-deep-1', { createdByCommitSha }, [
        B.folder('folder-deep-2', { createdByCommitSha }, [
          B.folder('folder-deep-3', { createdByCommitSha }, []),
        ]),
      ]),
    ]);

    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2'].location
    ).toEqual(
      DataLocation.makeFromNames([
        'folder-deep-1' as any,
        'folder-deep-2' as any,
      ])
    );
    const folderDeep2: IFolder = {
      type: 'folder',
      blobs: [],
      subFolders: [
        { type: 'folder', name: 'folder-deep-3' as any, createdByCommitSha },
      ],
    };
    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2'].asDataElement
    ).toEqual(folderDeep2);
    const folderElement: IFolderElement = {
      type: 'folder',
      createdByCommitSha,
      name: 'folder-deep-2' as any,
    };
    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2'].asFolderElement
    ).toEqual(folderElement);
    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2'].elements[
        'folder-deep-3'
      ]
    ).toBeTruthy();
  });
});
