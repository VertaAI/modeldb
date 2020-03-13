import { mount, ReactWrapper } from 'enzyme';
import * as React from 'react';

import * as Communication from 'core/shared/utils/redux/communication';
import { findByDataTestAttribute } from 'core/shared/utils/tests/react/helpers';

import WidgetBulkDeletion from '../WidgetBulkDeletion';

const makeComponent = (
  props: React.ComponentProps<typeof WidgetBulkDeletion>
) => {
  return mount(<WidgetBulkDeletion {...props} />);
};

const isShownWidgetDeletingIndicator = (
  component: ReactWrapper<any, any>
): boolean => {
  return (
    component.find('.entity_selected').length === 1 &&
    component.find('.togglerContainer').length === 1
  );
};

const WrappedWidget = (toggler: React.ReactNode) => (
  <div data-test="wrapped-widget">{toggler}</div>
);

describe('(components/BulkDeletion) WidgetBulkDeletion', () => {
  describe('when isEnabled is false', () => {
    it('should display only the wrapped widget', () => {
      const component = makeComponent({
        deleting: Communication.initialCommunication,
        id: 'id',
        isEnabled: false,
        isSelected: false,
        selectEntity: jest.fn(),
        unselectEntity: jest.fn(),
        children: WrappedWidget,
      });

      expect(
        findByDataTestAttribute('widget-bulk-deletion', component).length
      ).toBe(0);
      expect(
        findByDataTestAttribute('toggler-entity-for-deleting', component).length
      ).toBe(0);
      expect(findByDataTestAttribute('wrapped-widget', component).length).toBe(
        1
      );
    });
  });

  describe('when isEnabled is true', () => {
    it('should display wrapped widget with the deleting toggler', () => {
      const component = makeComponent({
        deleting: Communication.initialCommunication,
        id: 'id',
        isEnabled: true,
        isSelected: false,
        selectEntity: jest.fn(),
        unselectEntity: jest.fn(),
        children: WrappedWidget,
      });

      expect(
        findByDataTestAttribute('widget-bulk-deletion', component).length
      ).toBe(1);
      expect(
        findByDataTestAttribute('toggler-entity-for-deleting', component).length
      ).toBe(1);
    });

    describe('when isSelected is true', () => {
      it('should display the deleting indicator on the wrapped widget', () => {
        const component = makeComponent({
          deleting: Communication.initialCommunication,
          id: 'id',
          isEnabled: true,
          isSelected: true,
          selectEntity: jest.fn(),
          unselectEntity: jest.fn(),
          children: WrappedWidget,
        });

        expect(isShownWidgetDeletingIndicator(component)).toBeTruthy();
      });

      it('should make the wrapped widget unclickable', () => {
        const component = makeComponent({
          deleting: Communication.initialCommunication,
          id: 'id',
          isEnabled: true,
          isSelected: true,
          selectEntity: jest.fn(),
          unselectEntity: jest.fn(),
          children: WrappedWidget,
        });

        expect(
          findByDataTestAttribute('widget-bulk-deletion', component).hasClass(
            'muted_widget'
          )
        ).toBe(true);
      });

      it('should display the deleting toggler', () => {
        const component = makeComponent({
          deleting: Communication.initialCommunication,
          id: 'id',
          isEnabled: true,
          isSelected: true,
          selectEntity: jest.fn(),
          unselectEntity: jest.fn(),
          children: WrappedWidget,
        });

        expect(
          findByDataTestAttribute('toggler-entity-for-deleting', component)
            .length
        ).toBe(1);
      });

      it('should make the deleting toggler is clickable', () => {
        const component = makeComponent({
          deleting: Communication.initialCommunication,
          id: 'id',
          isEnabled: true,
          isSelected: true,
          selectEntity: jest.fn(),
          unselectEntity: jest.fn(),
          children: WrappedWidget,
        });

        expect(
          findByDataTestAttribute(
            'toggler-entity-for-deleting',
            component
          ).find('.unmuted_icon')
        ).toBeTruthy();
      });

      it('should not display the deleting indicator on the wrapped widget and should make the wrapped widget clickable when isSelected become false', () => {
        const component = makeComponent({
          deleting: Communication.initialCommunication,
          id: 'id',
          isEnabled: true,
          isSelected: true,
          selectEntity: jest.fn(),
          unselectEntity: jest.fn(),
          children: WrappedWidget,
        });

        component.setProps({ isSelected: false });
        component.update();

        expect(isShownWidgetDeletingIndicator(component)).toBeFalsy();
        expect(
          findByDataTestAttribute('widget-bulk-deletion', component).hasClass(
            'muted_widget'
          )
        ).toBe(false);
      });
    });
  });
});
