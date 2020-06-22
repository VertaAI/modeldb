import { FieldArray } from 'formik';
import * as React from 'react';

import TagsManager from 'shared/view/domain/BaseTagsManager/TagsManager';
import FieldWithTopLabel from 'shared/view/elements/FieldWithTopLabel/FieldWithTopLabel';

interface ILocalProps {
  name: string;
  label?: string;
}

export default class TagsField extends React.Component<ILocalProps> {
  private emptyArray = [];

  public render() {
    return (
      <FieldArray name={this.props.name}>
        {arrayHelpers => (
          <FieldWithTopLabel label={this.props.label || 'Tags'}>
            <TagsManager
              tags={
                arrayHelpers.form.values[arrayHelpers.name] || this.emptyArray
              }
              isAlwaysShowAddTagButton={true}
              isAvailableTagAdding={true}
              isDraggableTags={false}
              isRemovableTags={true}
              isShowPlaceholder={false}
              isUpdating={false}
              tagWordReplacer={this.props.label}
              onAddTag={tag => arrayHelpers.push(tag)}
              onRemoveTag={deletedTag =>
                arrayHelpers.remove(
                  arrayHelpers.form.values.tags.findIndex(
                    (tag: any) => tag === deletedTag
                  )
                )
              }
            />
          </FieldWithTopLabel>
        )}
      </FieldArray>
    );
  }
}
