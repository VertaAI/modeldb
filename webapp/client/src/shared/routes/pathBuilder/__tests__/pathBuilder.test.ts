import { IRepository } from 'shared/models/Versioning/Repository';
import { CommitPointer } from 'shared/models/Versioning/RepositoryData';

import { makePath, param, orParam, paramWithMod } from '..';

describe('pathBuilder', () => {
  it('should generate path from segments', () => {
    const res = makePath('repositories', 'create')();
    expect(res.value).toEqual('/repositories/create');
  });

  it('should generate path with simple params', () => {
    const res = makePath(
      'repositories',
      param('repositoryName')<IRepository['name']>(),
      'data',
      'compare',
      param('commitPointerAValue')<CommitPointer['value'] | string>()
    )();
    expect(res.value).toEqual(
      '/repositories/:repositoryName/data/compare/:commitPointerAValue'
    );
  });

  it('should generate path with or params', () => {
    const res = makePath(
      'repositories',
      param('repositoryName')<IRepository['name']>(),
      'data',
      orParam('dataType')(['blob', 'folder'])
    )();
    expect(res.value).toEqual(
      '/repositories/:repositoryName/data/:dataType(blob|folder)'
    );
  });

  it('should generate path with param with modifier', () => {
    const res = makePath(
      'repositories',
      paramWithMod('locationPathname', '*')<{ locationPathname: string }>()
    )();
    expect(res.value).toEqual('/repositories/:locationPathname*');
  });

  it('should generate path only without repetitive /', () => {
    expect(makePath('/repositories/', '///test')().value).toEqual(
      '/repositories/test'
    );
    expect(makePath('/')().value).toEqual('/');
  });
});
