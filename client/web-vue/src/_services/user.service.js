import {handleResponse, requestOptions, Utils} from '@/_helpers';

export const userService = {
    getAll,
    getById
};

function getAll() {
    return fetch(`${Utils.host()}/api/users`, requestOptions.get())
        .then(handleResponse);
}

function getById(id) {
    return fetch(`${Utils.host()}/api/users/${id}`, requestOptions.get())
        .then(handleResponse);
}

