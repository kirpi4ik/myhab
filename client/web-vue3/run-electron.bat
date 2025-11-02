@echo off
set APP_URL=file:///D:/git/myhab/client/web-vue3/dist/electron/UnPackaged/index.html
set QUASAR_ELECTRON_PRELOAD=preload/electron-preload.cjs
node_modules\.bin\electron.cmd dist\electron\UnPackaged --disable-gpu-shader-disk-cache --no-sandbox

