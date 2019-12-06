import { handleResponse, requestOptions } from '@/_helpers';

export const userService = {
    getAll,
    getById
};

function getAll() {
    return fetch(`${process.env.VUE_APP_SERVER_URL}/api/users`, requestOptions.get())
        .then(handleResponse);
}

function getById(id) {
    return fetch(`${process.env.VUE_APP_SERVER_URL}/api/users/${id}`, requestOptions.get())
        .then(handleResponse);
}