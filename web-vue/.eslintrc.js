module.exports = {
    "env": {
        "browser": true,
        "es6": true
    },
    "extends": [
        "eslint:recommended",
        "plugin:vue/essential"
    ],
    "globals": {
        "Atomics": "readonly",
        "SharedArrayBuffer": "readonly"
    },
    "parserOptions": {
        "parser": "babel-eslint",
        "ecmaVersion": 2018,
        "sourceType": "module",
        "allowImportExportEverywhere": true
    },
    "plugins": [
        "vue"
    ],
    "rules": {
        "no-console": "off",
        "no-debugger": "off",
        "no-unused-vars": "off",
        "no-undef": "off",
        "no-duplicate-attributes" : "off",
        "no-dupe-keys" : "off"
    }
};