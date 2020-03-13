const R = require('ramda');
const { deleteAllEntitiesFromAllWorkspaces, deleteProjectsByIds, getProjects, deleteAllOrganizations } = require('./helpers/userData');

const getConfig = require('./getConfig');

const config = getConfig();

const noop = () => {};

deleteAllEntitiesFromAllWorkspaces({ getAllEntities: getProjects, deleteAllEntities: deleteProjectsByIds })
    // .then(() => console.log('success'), (error) => console.log(error));
deleteAllOrganizations();
