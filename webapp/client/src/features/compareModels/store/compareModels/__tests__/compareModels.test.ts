import * as R from 'ramda';

import ModelRecord from 'shared/models/ModelRecord';
import { currentUser } from 'shared/utils/tests/mocks/models/users';

import { compareModels, getUrlForComparingCodeVersions } from '../compareModels';
import { makeGithubRemoteRepoUrl } from 'shared/utils/github/github';

const makePredefinedModel = (
  id: string,
  props: Partial<ModelRecord>
): ModelRecord => {
  return R.mergeLeft(props, {
    id,
    allowedActions: [],
    artifacts: [],
    attributes: [],
    datasets: [],
    dateCreated: new Date(),
    dateUpdated: new Date(),
    description: 'description',
    endTime: new Date(),
    experimentId: 'experimentId',
    hyperparameters: [],
    metrics: [],
    name: 'name',
    observations: [],
    owner: currentUser,
    ownerId: currentUser.id,
    projectId: 'projectId',
    shortExperiment: {
      id: 'df',
      name: 'cv',
    },
    startTime: new Date(),
    tags: [],
  }) as ModelRecord;
};

describe('(feature) compareEntities', () => {
  describe('property is true if it differs from any other entries in that row', () => {
    it('should compare id property', () => {
      const res = compareModels([
        makePredefinedModel('id-1', {}),
        makePredefinedModel('id-2', {}),
        makePredefinedModel('id-3', {}),
      ]);

      expect(res['id-1'].id).toEqual(true);
      expect(res['id-2'].id).toEqual(true);
      expect(res['id-3'].id).toEqual(true);
    });

    it('should compare owner property as true', () => {
      const res = compareModels([
        makePredefinedModel('id-1', {
          ownerId: 'owner-id-1',
        }),
        makePredefinedModel('id-2', {
          ownerId: 'owner-id-2',
        }),
        makePredefinedModel('id-3', {
          ownerId: 'owner-id-1',
        }),
      ]);

      expect(res['id-1'].ownerId).toEqual(true);
      expect(res['id-2'].ownerId).toEqual(true);
      expect(res['id-3'].ownerId).toEqual(true);
    });

    it('should compare owner property as false', () => {
      const res = compareModels([
        makePredefinedModel('id-1', {
          ownerId: 'owner-id-1',
        }),
        makePredefinedModel('id-2', {
          ownerId: 'owner-id-1',
        }),
        makePredefinedModel('id-3', {
          ownerId: 'owner-id-1',
        }),
      ]);

      expect(res['id-1'].ownerId).toEqual(false);
      expect(res['id-2'].ownerId).toEqual(false);
      expect(res['id-3'].ownerId).toEqual(false);
    });

    describe('hyperparameters', () => {
      it('should highlight background of hyperparameter value if hyperparameters with the same key have not the same value', () => {
        const res = compareModels([
          makePredefinedModel('id-2', {
            hyperparameters: [{ key: 'hyp-1', value: 125 }],
          }),
          makePredefinedModel('id-3', {
            hyperparameters: [{ key: 'hyp-1', value: 200 }],
          }),
        ]);

        expect(res['id-2'].hyperparameters).toEqual({ 'hyp-1': true });
        expect(res['id-3'].hyperparameters).toEqual({ 'hyp-1': true });
      });

      it('should highlight background of hyperparameter value if other models doesn`t have this hyperparameter', () => {
        const res = compareModels([
          makePredefinedModel('id-2', {
            hyperparameters: [
              { key: 'hyp-1', value: 125 },
              { key: 'hyp-2', value: 125 },
            ],
          }),
          makePredefinedModel('id-3', {
            hyperparameters: [{ key: 'hyp-3', value: 200 }],
          }),
        ]);

        expect(res['id-2'].hyperparameters).toEqual({
          'hyp-1': true,
          'hyp-2': true,
        });
        expect(res['id-3'].hyperparameters).toEqual({ 'hyp-3': true });
      });

      it('should not highlight background of hyperparameter value if all models have a hyperparater with the same value', () => {
        const res = compareModels([
          makePredefinedModel('id-2', {
            hyperparameters: [
              { key: 'hyp-1', value: 125 },
              { key: 'hyp-2', value: 150 },
            ],
          }),
          makePredefinedModel('id-3', {
            hyperparameters: [
              { key: 'hyp-1', value: 125 },
              { key: 'hyp-2', value: 150 },
            ],
          }),
        ]);

        expect(res['id-2'].hyperparameters).toEqual({
          'hyp-1': false,
          'hyp-2': false,
        });
        expect(res['id-3'].hyperparameters).toEqual({
          'hyp-1': false,
          'hyp-2': false,
        });
      });
    });

    describe('artifacts', () => {
      describe('should highlight border of artifact button if artifacts with the same keys have not the same type or path or linkedArtifactId or if artifact with key is not existed', () => {
        const res = compareModels([
          makePredefinedModel('id-1', {
            artifacts: [
              {
                key: 'diff-type',
                path: 'path-1',
                pathOnly: false,
                type: 'BINARY',
              },
              {
                key: 'diff-path',
                path: 'path-1',
                pathOnly: false,
                type: 'BINARY',
              },
              {
                key: 'diff-linkedArtifactId',
                path: 'path-1',
                pathOnly: false,
                type: 'BINARY',
                linkedArtifactId: 'linkedArtifactId',
              },
              {
                key: 'only-1-key',
                path: 'path-1',
                pathOnly: false,
                type: 'BINARY',
              },
              {
                key: 'the-same-artifact',
                path: 'path',
                pathOnly: false,
                type: 'BINARY',
              },
            ],
          }),
          makePredefinedModel('id-2', {
            artifacts: [
              {
                key: 'diff-type',
                path: 'path-1',
                pathOnly: false,
                type: 'BLOB',
              },
              {
                key: 'diff-path',
                path: 'path-adfadf',
                pathOnly: false,
                type: 'BINARY',
              },
              {
                key: 'diff-linkedArtifactId',
                path: 'path-1',
                pathOnly: false,
                type: 'BINARY',
                linkedArtifactId: 'linkedArtifactId-1',
              },
              {
                key: 'the-same-artifact',
                path: 'path',
                pathOnly: false,
                type: 'BINARY',
              },
            ],
          }),
        ]);

        expect(res['id-1'].artifacts).toEqual({
          'diff-type': true,
          'diff-path': true,
          'diff-linkedArtifactId': true,
          'only-1-key': true,
          'the-same-artifact': false,
        });
        expect(res['id-2'].artifacts).toEqual({
          'diff-type': true,
          'diff-path': true,
          'diff-linkedArtifactId': true,
          'the-same-artifact': false,
        });
      });
    });

    describe('attributes', () => {
      it('attribute is different if there are different type attributes', () => {
        const res = compareModels([
          makePredefinedModel('id-1', {
            attributes: [
              { key: 'primitive-key', value: 1 },
              { key: 'list-key', value: ['adf', 'zcv'] },
              { key: 'list-key-2', value: ['test', 'test2'] },
              { key: 'single-primitive-key', value: 2 },
              { key: 'missed-in-only-two-primitive-key', value: 1 }
            ],
          }),
          makePredefinedModel('id-2', {
            attributes: [
              { key: 'primitive-key', value: 'bla' },
              { key: 'list-key', value: 1 },
            ],
          }),
          makePredefinedModel('id-3', {
            attributes: [
              { key: 'primitive-key', value: 1 },
              { key: 'missed-in-only-two-primitive-key', value: 1 }
            ]
          }),
        ]);

        expect(res['id-1'].attributes).toEqual({
          'primitive-key': { type: 'primitive', isDiff: true },
          'list-key': { type: 'list', diffInfo: { adf: true, zcv: true } },
          'list-key-2': { type: 'list', diffInfo: { test: true, test2: true } },
          'single-primitive-key': { type: 'primitive', isDiff: true },
          'missed-in-only-two-primitive-key': { type: 'primitive', isDiff: true }
        });
        expect(res['id-2'].attributes).toEqual({
          'primitive-key': { type: 'primitive', isDiff: true },
          'list-key': { type: 'primitive', isDiff: true },
        });
        expect(res['id-3'].attributes).toEqual({
          'primitive-key': { type: 'primitive', isDiff: true },
          'missed-in-only-two-primitive-key': { type: 'primitive', isDiff: true }
        })
      });

      it('equal', () => {
        const res = compareModels([
          makePredefinedModel('id-1', {
            attributes: [
              { key: 'primitive-key', value: 1 },
              { key: 'list-key', value: ['adf', 'zcv'] },
            ],
          }),
          makePredefinedModel('id-2', {
            attributes: [
              { key: 'primitive-key', value: 1 },
              { key: 'list-key', value: ['adf', 'zcg'] },
            ],
          }),
        ]);

        expect(res['id-1'].attributes).toEqual({
          'primitive-key': { type: 'primitive', isDiff: false },
          'list-key': { type: 'list', diffInfo: { adf: false, zcv: true } },
        });
        expect(res['id-2'].attributes).toEqual({
          'primitive-key': { type: 'primitive', isDiff: false },
          'list-key': { type: 'list', diffInfo: { adf: false, zcg: true } },
        });
      });
    });

    describe('code versions', () => {
      it('should be different if code versions types are different', () => {
        const res = compareModels([
          makePredefinedModel('id-1', {
            codeVersion: {
              type: 'git',
              data: {
                commitHash: 'commitHash',
              }
            }
          }),
          makePredefinedModel('id-2', {
            codeVersion: {
              type: 'artifact',
              data: {
                key: 'key',
                path: 'path',
                pathOnly: false,
                type:'BINARY',
              }
            }
          }),
        ]);

        expect(res['id-1'].codeVersion).toEqual({
          type: 'gitCodeVersion',
          diffInfoByKeys: {
            commitHash: true,
          }
        });
        expect(res['id-2'].codeVersion).toEqual({
          type: 'artifactCodeVersion',
          diffInfoByKeys: {
            key: true,
            path: true,
            type: true
          }
        });
      });

      describe('artifact code versions', () => {
        it('should not be different if path, type and key is not different', () => {
          const res = compareModels([
            makePredefinedModel('id-1', {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'key',
                  path: 'path',
                  pathOnly: false,
                  type: 'BINARY',
                },
              },
            }),
            makePredefinedModel('id-2', {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'key',
                  path: 'path',
                  pathOnly: false,
                  type: 'BINARY',
                },
              },
            }),
          ]);

          expect(res['id-1'].codeVersion).toEqual({
            type: 'artifactCodeVersion',
            diffInfoByKeys: {
              key: false,
              path: false,
              type: false,
            },
          });
        });

        it('should be different if path, key or type is different', () => {
          const res = compareModels([
            makePredefinedModel('id-1', {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'diffPath',
                  path: 'path',
                  pathOnly: false,
                  type: 'BLOB',
                },
              },
            }),
            makePredefinedModel('id-2', {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'diffPath',
                  path: 'path123',
                  pathOnly: false,
                  type: 'BINARY',
                },
              },
            }),
          ]);

          expect(res['id-1'].codeVersion).toEqual({
            type: 'artifactCodeVersion',
            diffInfoByKeys: {
              key: false,
              path: true,
              type: true,
            },
          });
        });

        it('should be different if different types', () => {
          const res = compareModels([
            makePredefinedModel('id-1', {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'diffPath',
                  path: 'path',
                  pathOnly: false,
                  type: 'BINARY',
                },
              },
            }),
            makePredefinedModel('id-2', {
              codeVersion: {
                type: 'git',
                data: {},
              },
            }),
          ]);

          expect(res['id-1'].codeVersion).toEqual({
            type: 'artifactCodeVersion',
            diffInfoByKeys: {
              key: true,
              path: true,
              type: true,
            },
          });
        });

        it('should be different if only 1', () => {
          const res = compareModels([
            makePredefinedModel('id-1', {
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'diffPath',
                  path: 'path',
                  pathOnly: false,
                  type: 'BINARY',
                },
              },
            }),
            makePredefinedModel('id-2', {
              codeVersion: undefined,
            }),
          ]);

          expect(res['id-1'].codeVersion).toEqual({
            type: 'artifactCodeVersion',
            diffInfoByKeys: {
              key: true,
              path: true,
              type: true,
            },
          });
        });
      });

      describe('git code versions', () => {
        it('should be not different if exec path or remote repo url or commit hash is the same', () => {
          const res = compareModels([
            makePredefinedModel('id-1', {
              codeVersion: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath',
                  remoteRepoUrl: makeGithubRemoteRepoUrl(
                    'git@github.com:VertaAI/modeldb-client.git'
                  ),
                  isDirty: 'FALSE',
                },
              },
            }),
            makePredefinedModel('id-2', {
              codeVersion: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath',
                  remoteRepoUrl: makeGithubRemoteRepoUrl(
                    'git@github.com:VertaAI/modeldb-client.git'
                  ),
                  isDirty: 'FALSE',
                },
              },
            }),
          ]);

          expect(res['id-1'].codeVersion).toEqual({
            type: 'gitCodeVersion',
            diffInfoByKeys: {
              commitHash: false,
              execPath: false,
              remoteRepoUrl: false,
              isDirty: false,
            },
          });
        });

        it('should be different if exec path or remote repo url or commit hash is different', () => {
          const res = compareModels([
            makePredefinedModel('id-1', {
              codeVersion: {
                type: 'git',
                data: {
                  commitHash: 'commitHash',
                  execPath: 'exectPath',
                  remoteRepoUrl: makeGithubRemoteRepoUrl(
                    'git@github.com:VertaAI/modeldb-client.git'
                  ),
                  isDirty: 'FALSE',
                },
              },
            }),
            makePredefinedModel('id-2', {
              codeVersion: {
                type: 'git',
                data: {
                  commitHash: 'anotherCommitHash',
                  execPath: 'anotherExectPath',
                  remoteRepoUrl: makeGithubRemoteRepoUrl(
                    'git@github.com:VertaAI/another-repo-url.git'
                  ),
                  isDirty: 'TRUE',
                },
              },
            }),
          ]);

          expect(res['id-1'].codeVersion).toEqual({
            type: 'gitCodeVersion',
            diffInfoByKeys: {
              commitHash: true,
              execPath: true,
              remoteRepoUrl: true,
              isDirty: true,
            },
          });
        });
      });

      describe('getUrlForComparingCodeVersions', () => {
        it('should return undefined if code versions more than 2', () => {
          const res = getUrlForComparingCodeVersions([
            {
              id: 'id1',
              codeVersion: {
                type: 'git',
                data: {},
              }
            },
            {
              id: 'id2',
              codeVersion: {
                type: 'git',
                data: {},
              }
            },
            {
              id: 'id3',
              codeVersion: {
                type: 'git',
                data: {}
              }
            }
          ]);

          expect(res).toEqual(undefined);          
        })

        it('should return undefined if all types of code versions are not the same', () => {
          const res = getUrlForComparingCodeVersions([
            {
              id: 'id1',
              codeVersion: {
                type: 'git',
                data: {},
              }
            },
            {
              id: 'id2',
              codeVersion: {
                type: 'artifact',
                data: {
                  key: 'key',
                  path: 'path',
                  pathOnly: false,
                  type: 'BINARY'
                },
              }
            },
          ]);

          expect(res).toEqual(undefined);
        });

        describe('git code versions', () => {
          const remoteRepoUrl1 = makeGithubRemoteRepoUrl(
            'git@github.com:VertaAI/modeldb-client.git'
          );
          const remoteRepoUrl2 = makeGithubRemoteRepoUrl(
            'git@github.com:NotVertaAI/not-modeldb-client.git'
          );

          it('should return undefined if remote repo urls are not equals', () => {
            const res = getUrlForComparingCodeVersions([
              {
                id: 'id1',
                codeVersion: {
                  type: 'git',
                  data: {
                    remoteRepoUrl: remoteRepoUrl2
                  },
                }
              },
              {
                id: 'id2',
                codeVersion: {
                  type: 'git',
                  data: { remoteRepoUrl: remoteRepoUrl1 },
                }
              },
            ]);

            expect(res).toEqual(undefined);
          });

          it('should return undefined if there are not commit hashes', () => {
            const res = getUrlForComparingCodeVersions([
              {
                id: 'id1',
                codeVersion: {
                  type: 'git',
                  data: {
                    remoteRepoUrl: remoteRepoUrl1,
                  },
                }
              },
              {
                id: 'id2',
                codeVersion: {
                  type: 'git',
                  data: { remoteRepoUrl: remoteRepoUrl1, commitHash: 'bla2' },
                }
              },
            ]);

            expect(res).toEqual(undefined);
          });

          it('should return url for comparing git code versions commits if remote repo urls are the same and there are commit hashes', () => {
            const res = getUrlForComparingCodeVersions([
              {
                id: 'id3',
                codeVersion: {
                  type: 'git',
                  data: {
                      remoteRepoUrl: remoteRepoUrl1,
                      commitHash: '0db2e34e00f34fca818d49f682fbcecfc20514a6',
                  }
                }
              },
              {
                id: 'id2',
                codeVersion: {
                  type: 'git',
                  data: {
                    remoteRepoUrl: remoteRepoUrl1,
                    commitHash: 'afd13413fadff34fca818d49f682fbcecfc20514a6',
                  },
                }
              },
            ]);

            expect(res).toEqual('https://github.com/VertaAI/modeldb-client/compare/0db2e3..afd134')
          });
        });
      });
    });

    describe('datasets', () => {
      it('should compare key, type, linkedArtifactId and path', () => {
        const res = compareModels([
          makePredefinedModel('id-1', {
            datasets: [
              { type: 'BINARY', key: 'key', path: 'path', pathOnly: false, linkedArtifactId: 'linkedArtifactId' },
              { type: 'BINARY', key: 'equal-key', path: 'path', pathOnly: false, linkedArtifactId: 'linkedArtifactId' },
            ],
          }),
          makePredefinedModel('id-2', {
            datasets: [
              { type: 'BLOB', key: 'key', path: 'path2', pathOnly: false, linkedArtifactId: 'linkedArtifactId-2' },
              { type: 'BINARY', key: 'equal-key', path: 'path', pathOnly: false, linkedArtifactId: 'linkedArtifactId' },
            ]
          }),
          makePredefinedModel('id-3', {
            datasets: [
              { type: 'BINARY', key: 'key', path: 'path', pathOnly: false, linkedArtifactId: 'linkedArtifactId' },
              { type: 'BINARY', key: 'equal-key', path: 'path', pathOnly: false, linkedArtifactId: 'linkedArtifactId' },
            ]
          }),
        ]);

        expect(res['id-1'].datasets).toEqual({
            key: {
              type: true, key: false, path: true, linkedArtifactId: true
            },
            'equal-key': {
              type: false, key: false, path: false, linkedArtifactId: false
            },
        });
      });
    });
  });
});
