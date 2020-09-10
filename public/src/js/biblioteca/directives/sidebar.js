(function() {
  'use strict';

  angular
    .module('material-lite')
    .directive('mlSidebar', mlSidebar);

  function mlSidebar() {
    return {
      restrict: 'E',
      templateUrl: 'assets/src/tpl/biblioteca/partials/sidebar.html',
      replace: true
    };
  }

})();
