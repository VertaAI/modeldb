import {
  findByDataTestAttribute,
  makeAsyncInputHelpersByName,
} from 'core/shared/utils/tests/react/helpers';

import { ReactWrapper } from 'enzyme';

export const makeTagsManagerHelpers = () => {
  const find = findByDataTestAttribute('tags-manager');
  const findTagsWrappers = findByDataTestAttribute('tags-manager-tag');
  const findTags = (component: ReactWrapper) =>
    findTagsWrappers(component).map(w => w.text());
  const addTag = async (tag: string, component: ReactWrapper) => {
    findByDataTestAttribute(
      'tags-manager-open-tag-creator-button',
      component
    ).simulate('click');
    component.update();

    const tagField = makeAsyncInputHelpersByName('tag-submitted');
    await tagField.changeAndBlur(tag, component);

    findByDataTestAttribute('tags-manager-create-button', component).simulate(
      'click'
    );
    component.update();
  };
  const deleteTag = async (tag: string, component: ReactWrapper) => {
    findByDataTestAttribute(
      'tags-manager-open-tag-deletion-confirm',
      component
    ).simulate('click');
    component.update();

    findByDataTestAttribute('confirm-ok-button', component).simulate('click');
    component.update();
  };

  return {
    find,
    findTagsWrappers,
    addTag,
    deleteTag,
    findTags,
  };
};
