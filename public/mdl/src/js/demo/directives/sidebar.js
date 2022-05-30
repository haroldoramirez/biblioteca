(function() {
  'use strict';

  angular
    .module('material-lite')
    .directive('mlSidebar', mlSidebar);

  function mlSidebar() {
    return {
      restrict: 'E',
      templateUrl: 'assets/mdl/src/tpl/demo/partials/sidebar.html',
      replace: true
    };
  }

})();
