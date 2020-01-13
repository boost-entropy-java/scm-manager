module.exports = api => {
  return {
    presets: [
      require("@babel/preset-env"),
      require("@babel/preset-flow"),
      require("@babel/preset-react"),
      require("@babel/preset-typescript")
    ],
    plugins: [require("@babel/plugin-proposal-class-properties"), require("@babel/plugin-proposal-optional-chaining")]
  };
};
