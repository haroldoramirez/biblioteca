(function() {
  'use strict';

  angular
    .module('material-lite')
    .directive('mlHeader', mlHeader);

  function mlHeader() {
    return {
      restrict: 'E',
      templateUrl: 'assets/src/tpl/biblioteca/partials/header.html',
      replace: true
    };
  }

})();
