angular.module('biblioteca')
    .controller('registros.controller', function ($scope, $sce) {
        $scope.init = function() {
            $scope.url = $sce.trustAsResourceUrl('https://mapbiogas.cibiogas.org');
        }
    });
