const { getDefaultConfig } = require('@expo/metro-config');

module.exports = (async () => {
  const defaultConfig = await getDefaultConfig(__dirname);
  return {
    ...defaultConfig,
    resolver: {
      sourceExts: ['jsx', 'json', 'ts', 'tsx'],
    assetExts: ['png', 'jpg', 'jpeg', 'svg'],
    platforms: ['android', 'ios'],
  };
})();
