(function() {
    'use strict';

    // routes
    angular
        .module('material-lite')
        .config(['$routeProvider', routeProvider])
        .run(['$route', routeRunner]);

    function routeProvider($routeProvider) {

        $routeProvider
            .when('/', {
                templateUrl: 'assets/src/tpl/biblioteca/dashboard.html'
            }).when('#/gallery', {
                templateUrl: 'assets/src/tpl/biblioteca/gallery.html'
            });
    }

    function routeRunner($route) {
        // $route.reload();
    }

})();
