import { IArtifact } from 'core/shared/models/Artifact';

import {
  ArtifactPreviewFileExtension,
  ArtifactPreviewFileExtensions,
} from './types';

export const checkSupportArtifactPreview = (artifact: IArtifact) =>
  !artifact.pathOnly && Boolean(getArtifactPreviewFileExtension(artifact));

export const getArtifactPreviewFileExtension = (
  artifact: IArtifact
): ArtifactPreviewFileExtension | null => {
  return artifact.fileExtension &&
    Object.keys(ArtifactPreviewFileExtensions).includes(artifact.fileExtension)
    ? (artifact.fileExtension as ArtifactPreviewFileExtension)
    : null;
};
