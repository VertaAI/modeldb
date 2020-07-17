import * as React from 'react';

import {
  ArtifactCodeVersionPile,
  ArtifactCodeVersionFields,
} from 'shared/view/domain/CodeVersion/ArtifactCodeVersion/ArtifactCodeVersion';
import {
  GitCodeVersionPile,
  GitCodeVersionPopupFields,
} from 'shared/view/domain/CodeVersion/GitCodeVersionButton/GitCodeVersionButton';
import {
  ICodeVersion,
} from 'shared/models/CodeVersion';
import PilePopup from 'shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';
import { IModelDifferentProps, oneOfKeyIsDiff, getUrlForComparingCodeVersions } from 'features/compareModels/store/compareModels/compareModels';
import Button from 'shared/view/elements/Button/Button';

import { IEntityInfo } from '../CompareModelsTable/CompareModelsTable';
import { getDiffValueBorderClassname, getDiffValueBgClassname } from '../../shared/DiffHighlight/DiffHighlight';
import { PopupComparedEntities } from '../../shared/PopupComparedEntities/PopupComparedEntities';

interface ILocalProps {
  currentEntityInfo: IEntityInfo<ICodeVersion, IModelDifferentProps['codeVersion']>;
  allEntitiesInfo: Array<IEntityInfo<ICodeVersion | undefined, IModelDifferentProps['codeVersion']>>;
}

class ComparableCodeVersion extends React.PureComponent<ILocalProps> {
  public render() {
    const { currentEntityInfo } = this.props;
    const PileComponent =
      currentEntityInfo.value.type === 'artifact'
        ? ArtifactCodeVersionPile
        : GitCodeVersionPile;

    return (
      <PileWithPopup
        pileComponent={({ showPopup }) => (
          <PileComponent
            additionalClassname={getDiffValueBorderClassname(
              currentEntityInfo.model.modelNumber,
              oneOfKeyIsDiff(currentEntityInfo.diffInfo?.diffInfoByKeys ?? {})
            )}
            onClick={showPopup}
          />
        )}
        popupComponent={({ closePopup, isOpen }) =>
          isOpen ? (
            <CompareCodeVersionsPopup
              currentEntityInfo={this.props.currentEntityInfo}
              allEntitiesInfo={this.props.allEntitiesInfo}
              isOpen={true}
              onClose={closePopup}
            />
          ) : null
        }
      />
    );
  }
}

class CompareCodeVersionsPopup extends React.PureComponent<{
  isOpen: boolean;
  onClose(): void;
} & ILocalProps> {
  public render() {
    const {
      allEntitiesInfo,
      isOpen,
      onClose,
    } = this.props;

    const urlForComparingCodeVersions = getUrlForComparingCodeVersions(allEntitiesInfo.map(({ model: { id }, value }) => value ? ({ id, codeVersion: value }) : undefined));

    return (
      <PilePopup
        isOpen={isOpen}
        title="Comparing code versions"
        titleIcon="codepen"
        width={760}
        onRequestClose={onClose}
      >
        <PopupComparedEntities entities={allEntitiesInfo.map((x) => x.value && x.diffInfo ? ({ value: x.value!, entityType: x.entityType, diffInfo: x.diffInfo }) : undefined)}>
          {(entityInfo) => (
            entityInfo.value.type === 'artifact'
                ? (
                  <ArtifactCodeVersionFields
                    getAdditionalValueClassname={field => getDiffValueBgClassname(entityInfo.entityType, entityInfo.diffInfo?.diffInfoByKeys[field] ?? false)}
                    artifactCodeVersion={entityInfo.value}
                  />
                )
                : (
                  <GitCodeVersionPopupFields
                    getAdditionalValueClassname={field => getDiffValueBgClassname(entityInfo.entityType, entityInfo.diffInfo?.diffInfoByKeys[field] ?? false)}
                    gitCodeVersion={entityInfo.value}
                  />
                )
          )}
        </PopupComparedEntities>
        {urlForComparingCodeVersions && (
          <PilePopup.Actions>
            <Button
              dataTest="compare-code-versions-button"
              onClick={() => window.open(urlForComparingCodeVersions, '_blank')}
            >
              Compare Code
            </Button>
          </PilePopup.Actions>
        )}
      </PilePopup>
    );
  }
}

export default ComparableCodeVersion;
