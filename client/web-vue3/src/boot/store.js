import { boot } from 'quasar/wrappers';

export default boot(({ app, store }) => {
  // Explicitly provide store for composition API
  // This ensures useStore() works in all components
  app.provide('store', store);
});
