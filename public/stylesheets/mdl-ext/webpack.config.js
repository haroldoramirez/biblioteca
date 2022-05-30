if (!global.Promise) {
  console.log("require es6-promise");
  global.Promise = require('es6-promise').polyfill();
}

const webpack = require('webpack');
const path = require('path');
const autoprefixer = require('autoprefixer');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const StyleLintPlugin = require('stylelint-webpack-plugin');
const env = require('yargs').argv.mode;

const scss = [
  'css-loader?sourceMap',
  'postcss-loader',
  'resolve-url-loader',
  'sass-loader?sourceMap&expanded'
].join('!');

const libraryName = 'mdl-ext';
const cssName = 'mdl-ext';
var outputFile;
var outputCss;
var outputCssEqJs;

if (env === 'build') {
  outputFile = libraryName + '.min.js';
  outputCss = '[name].min.css';
  outputCssEqJs = cssName + '-eqjs.min.css';
}
else {
  outputFile = libraryName + '.js';
  outputCss = '[name].css';
  outputCssEqJs = cssName + '-eqjs.css';
}
var prodPlugins = env === 'build' ? [
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        unused: true,    // Enables tree shaking
        dead_code: true, // Enables tree shaking
        pure_getters: true,
        warnings: false,
        screw_ie8: true,
        conditionals: true,
        comparisons: true,
        sequences: true,
        evaluate: true,
        join_vars: true,
        if_return: true,
      },
      output: {
        comments: false
      },
      sourceMap: true
    }),
  ] : [];

var config = {
  entry: {
    'mdl-ext': [
      path.join(__dirname, 'src/mdl-ext-build.scss'),      // MDLEXT Styles
      path.join(__dirname, 'src/index.js')                 // MDLEXT scripts
    ],
    'mdl-ext-eqjs': [
      path.join(__dirname, 'src/mdl-ext-eqjs-build.scss'), // MDLEXT Styles based on eq.js
      path.join(__dirname, 'src/index.js')                 // MDLEXT scripts
    ],
  },
  devtool: 'source-map',
  output: {
    path: path.join(__dirname, 'lib'),
    filename: outputFile,
    library: libraryName,
    libraryTarget: 'umd',
    umdNamedDefine: true
  },
  module: {
    preLoaders: [
      {
        loader: 'eslint',
        test: /\.js[x]?$/,
        include: [                     // ... or: exclude: /(node_modules|bower_components)/,
          path.join(__dirname, 'src'),
          path.join(__dirname, 'test')
        ]
      }
    ],
    loaders: [
      {
        test: /\.js[x]?$/,
        loader: 'babel',
        exclude: /(node_modules|bower_components)/
      },
      {
        test: /\.scss$/,
        include: path.join(__dirname, 'src'),
        loader: ExtractTextPlugin.extract('style-loader', scss)
      },
    ]
  },
  sassLoader: {
    includePaths: [
      path.resolve(__dirname, './node_modules'),
      path.resolve(__dirname, './src')
    ]
  },
  postcss: [
    autoprefixer({
      browsers: ['last 2 versions']
    })
  ],
  eslint: {
    // config in '.eslintrc'
    failOnWarning: false,
    failOnError: true
  },
  resolve: {
    root: path.resolve('./src'),
    modulesDirectories: ['src', 'node_modules'],
    extensions: ['', '.js', '.jsx', '.css', '.scss', 'html']
  },
  plugins: [
    new ExtractTextPlugin(outputCss, {
      disable: false,
      allChunks: true
    }),
    new StyleLintPlugin({
      // http://stylelint.io/user-guide/example-config/
      configFile: '.stylelintrc',
      context: 'src',
      files: '**/*.s?(a|c)ss',
      syntax: 'scss',
      failOnError: false
    }),
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.OccurrenceOrderPlugin()
  ].concat(prodPlugins),
};

module.exports = config;
