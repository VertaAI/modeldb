import { FieldArray } from 'formik';
import * as React from 'react';

import TagsManager from 'core/shared/view/domain/TagsManager/TagsManager';
import FieldWithTopLabel from 'core/shared/view/elements/FieldWithTopLabel/FieldWithTopLabel';

export default class TagsField extends React.Component<{ name: string }> {
  private emptyArray = [];

  public render() {
    return (
      <FieldArray name={this.props.name}>
        {arrayHelpers => (
          <FieldWithTopLabel label="Tags">
            <TagsManager
              tags={
                arrayHelpers.form.values[arrayHelpers.name] || this.emptyArray
              }
              isAvailableTagAdding={true}
              isDraggableTags={false}
              isRemovableTags={true}
              isShowPlaceholder={false}
              isUpdating={false}
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
