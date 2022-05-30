(function() {
  'use strict';

  angular
    .module('material-lite')
    .directive('mlHeader', mlHeader);

  function mlHeader() {
    return {
      restrict: 'E',
      templateUrl: 'assets/mdl/src/tpl/demo/partials/header.html',
      replace: true
    };
  }

})();
