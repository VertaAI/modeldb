import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
import {
  IFolder,
  ISubFolderElement,
  IFolderElement,
} from 'shared/models/Versioning/RepositoryData';
import { S3DatasetBlob } from 'shared/utils/tests/mocks/models/Versioning/blobMocks';

import * as B from '../index';

describe('commitTreeBuilder', () => {
  it('should create the root', () => {
    const root = B.root([
      B.folder('folder-deep-1', [
        B.folder('folder-deep-2', []),
        B.blob('blob-deep-2', S3DatasetBlob),
      ]),
    ]);

    expect(root.elements['folder-deep-1'].location).toEqual(
      CommitComponentLocation.makeFromNames(['folder-deep-1' as any])
    );
    const rootFolder: IFolder = {
      type: 'folder',
      blobs: [
        {
          type: 'blob',
          name: 'blob-deep-2' as any,
        },
      ],
      subFolders: [
        {
          type: 'folder',
          name: 'folder-deep-2' as any,
        },
      ],
    };
    expect(root.elements['folder-deep-1'].asCommitElement).toEqual(rootFolder);
    const folderElement: ISubFolderElement = {
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
      B.folder('folder-deep-1', [
        B.folder('folder-deep-2', [B.folder('folder-deep-3', [])]),
      ]),
    ]);

    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2'].location
    ).toEqual(
      CommitComponentLocation.makeFromNames([
        'folder-deep-1' as any,
        'folder-deep-2' as any,
      ])
    );
    const folderDeep2: IFolder = {
      type: 'folder',
      blobs: [],
      subFolders: [{ type: 'folder', name: 'folder-deep-3' as any }],
    };
    expect(
      root.elements['folder-deep-1'].elements['folder-deep-2'].asCommitElement
    ).toEqual(folderDeep2);
    const folderElement: IFolderElement = {
      type: 'folder',
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
