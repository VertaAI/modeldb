import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router';
import { Link } from 'react-router-dom';
import routes, { GetRouteParams } from 'shared/routes';

import { makeDefaultMetricFilter } from 'shared/models/Filters';
import { withScientificNotationOrRounded } from 'shared/utils/formatters/number';
import Draggable from 'shared/view/elements/Draggable/Draggable';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import TagBlock from 'shared/view/elements/TagBlock/TagBlock';

import closeIcon from './images/close.svg';
import styles from './ModelRecordCard.module.css';

interface IKeyValPair {
  key: string;
  value: number | string;
}

interface IModelRecordObj {
  name: string;
  timestamp: string;
  projectId: string;
  id: string;
  tags: string[];
  metrics: [IKeyValPair];
  hyper: [IKeyValPair];
}

interface ILocalProps {
  isOpen: boolean;
  data: IModelRecordObj;
  onRequestClose(): void;
}

const ModelProperty: React.SFC<{ property: IKeyValPair; type: string }> = ({
  property,
  type,
}) => {
  return (
    <Draggable
      additionalClassName={styles.param_draggable}
      type="filter"
      data={makeDefaultMetricFilter(`${type}.${property.key}`, property.value)}
    >
      <div className={styles.paramCell}>
        <div className={styles.paramKey}>{property.key}</div>
        <div className={styles.paramVal}>
          {typeof property.value === 'number'
            ? withScientificNotationOrRounded(Number(property.value))
            : property.value}
        </div>
      </div>
    </Draggable>
  );
};

type AllProps = ILocalProps &
  RouteComponentProps<GetRouteParams<typeof routes.modelRecord>>;

class ModelRecordCard extends React.Component<AllProps> {
  public render() {
    const { isOpen, onRequestClose, data } = this.props;
    return (
      <div className={isOpen ? styles.show : styles.hide}>
        <div className={styles.modelCardContent}>
          <div className={styles.header}>
            <img
              className={styles.close}
              src={closeIcon}
              onClick={onRequestClose}
              alt="close-icon"
            />
          </div>
          {data && data.id ? (
            <div className={styles.cardField}>
              <div className={styles.cardFieldLabel}>Name:</div>
              <Link
                className={styles.cardFieldValue_Link}
                to={routes.modelRecord.getRedirectPathWithCurrentWorkspace({
                  projectId: this.props.match.params.projectId,
                  modelRecordId: data.id,
                })}
              >
                <div className={styles.cardFieldValue}>
                  <span>{data.name}</span>
                </div>
              </Link>
            </div>
          ) : (
            ''
          )}

          <div className={styles.cardField}>
            <div className={styles.cardFieldLabel}>{`Timestamp:`}</div>
            <div className={styles.cardFieldValue}>
              <span>{data.timestamp}</span>
            </div>
          </div>

          {data.tags && data.tags.length > 0 && (
            <div className={styles.cardField}>
              <div className={styles.cardFieldLabel}>{`Tags:`}</div>
              <div className={styles.cardFieldValue}>
                <TagBlock tags={data.tags} isDraggable={true} />
              </div>
            </div>
          )}

          <div className={styles.cardField}>
            {data.metrics.length > 0 && (
              <div className={styles.cardFieldLabel}>Metrics: </div>
            )}
            <ScrollableContainer
              maxHeight={100}
              containerOffsetValue={12}
              minRowCount={3}
              elementRowCount={data.metrics.length}
              hiddenChildren={
                <div>
                  {data.metrics.map((d: IKeyValPair, i: number) => {
                    return (
                      <ModelProperty key={i} property={d} type={'metrics'} />
                    );
                  })}
                </div>
              }
            />
          </div>

          <div className={styles.cardField}>
            {data.hyper.length > 0 && (
              <div className={styles.cardFieldLabel}>Hyperpameters: </div>
            )}
            <ScrollableContainer
              maxHeight={140}
              containerOffsetValue={12}
              minRowCount={3}
              elementRowCount={data.hyper.length}
              hiddenChildren={
                <div>
                  {data.hyper.map((d: IKeyValPair, i: number) => {
                    return (
                      <ModelProperty
                        key={i}
                        property={d}
                        type={'hyperparameters'}
                      />
                    );
                  })}
                </div>
              }
            />
          </div>
        </div>
      </div>
    );
  }
}

export default withRouter(ModelRecordCard);
