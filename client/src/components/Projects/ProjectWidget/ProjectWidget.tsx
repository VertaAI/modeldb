import { bind } from 'decko';
import * as React from 'react';
import Avatar from 'react-avatar';
import { Link } from 'react-router-dom';
import { connect } from 'react-redux';
import cn from 'classnames';

import Tag from 'components/shared/TagBlock/TagProject';
import SharePopup from 'components/SharePopup/SharePopup';
import { Project, UserAccess } from 'models/Project';
import User from 'models/User';
import routes from 'routes';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import {
  loadCollaboratorsWithOwner,
  selectIsLoadingProjectCollaboratorsWithOwner,
} from 'store/collaboration';

import combined from './images/combined.svg';
import styles from './ProjectWidget.module.css';
import Preloader from 'components/shared/Preloader/Preloader';

interface ILocalProps {
  project: Project;
}

interface IStateFromProps {
  isLoadingCollaboratorsWithOwner: boolean;
}

interface ILocalState {
  showModal: boolean;
}

type AllProps = ILocalProps & IStateFromProps & IConnectedReduxProps;

class ProjectWidget extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = { showModal: false };

  public componentDidMount() {
    this.props.dispatch(loadCollaboratorsWithOwner(this.props.project));
  }

  public render() {
    const { project, isLoadingCollaboratorsWithOwner } = this.props;
    const showCollaboratorsAvatars = project.collaborators.size > 1;
    const moreThanMaxCollaborators = project.collaborators.size - 3;

    return (
      <div>
        <SharePopup
          project={project}
          showModal={this.state.showModal}
          onRequestClose={this.handleCloseModal}
        />
        <Link
          className={cn(styles.project_link, {
            [styles.loading_collaborators_with_owner]: isLoadingCollaboratorsWithOwner,
          })}
          to={routes.charts.getRedirectPath({ projectId: project.id })}
        >
          <div className={styles.project_widget}>
            <div className={styles.title_block}>
              <div className={styles.title}>{project.name}</div>
              <div className={styles.description}>{project.description}</div>
            </div>
            <div className={styles.tags_block}>
              {this.props.project.tags &&
                this.props.project.tags.map((tag: string, i: number) => (
                  <Tag tag={tag} key={i} />
                ))}
            </div>
            <div className={styles.metrics_block} />
            <div className={styles.author_block}>
              {this.props.isLoadingCollaboratorsWithOwner ? (
                <Preloader variant="dots" />
              ) : (
                <>
                  <div className={styles.author_name}>
                    <div>{project.Author.getNameOrEmail()}</div>
                    <div className={styles.author_status}>Owner</div>
                  </div>
                  <Avatar
                    name={project.Author.getNameOrEmail()}
                    round={true}
                    size="36"
                    textSizeRatio={36 / 16}
                    className={styles.author_avatar}
                    src={project.Author.picture ? project.Author.picture : ''}
                  />
                </>
              )}
            </div>
            <div className={styles.collaborators}>
              {this.props.isLoadingCollaboratorsWithOwner ? (
                <Preloader variant="dots" />
              ) : (
                <button
                  className={styles.collaborate_button}
                  style={{ paddingRight: showCollaboratorsAvatars ? 8 : 0 }}
                  onClick={this.showCollaborators}
                >
                  {showCollaboratorsAvatars ? (
                    ''
                  ) : (
                    <img src={combined} className={styles.combined_icon} />
                  )}
                  <span
                    style={{ marginLeft: showCollaboratorsAvatars ? 14 : 4 }}
                  >
                    Collaborators
                  </span>
                  {showCollaboratorsAvatars ? (
                    <span>
                      {Array.from(project.collaborators.keys()).map(
                        (user: User, index: number) => {
                          return index < 3 ? (
                            <Avatar
                              key={index}
                              name={user.getNameOrEmail()}
                              round={true}
                              size="24"
                              textSizeRatio={24 / 11}
                              className={styles.collaborator_avatar}
                              src={user.picture ? user.picture : ''}
                            />
                          ) : (
                            ''
                          );
                        }
                      )}
                      {project.collaborators.size > 3 ? (
                        <Avatar
                          name={`+ ${moreThanMaxCollaborators}`}
                          round={true}
                          size="24"
                          textSizeRatio={24 / 11}
                          className={styles.collaborator_avatar}
                          color="#EDEDED"
                          fgColor="#666666"
                        />
                      ) : (
                        ''
                      )}
                    </span>
                  ) : (
                    ''
                  )}
                </button>
              )}
            </div>
            <div className={styles.created_date_block}>
              <div className={styles.created_date}>
                Created: {project.dateCreated.toLocaleDateString()}
              </div>
              <div>Updated: {project.dateUpdated.toLocaleDateString()}</div>
            </div>
          </div>
        </Link>
      </div>
    );
  }

  @bind
  private showCollaborators(event: React.SyntheticEvent<HTMLButtonElement>) {
    event.preventDefault();
    this.setState({ showModal: true });
  }

  @bind
  private handleCloseModal() {
    this.setState({ showModal: false });
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IStateFromProps => {
  return {
    isLoadingCollaboratorsWithOwner: selectIsLoadingProjectCollaboratorsWithOwner(
      state,
      localProps.project.id
    ),
  };
};

export default connect(mapStateToProps)(ProjectWidget);
