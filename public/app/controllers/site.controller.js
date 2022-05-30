angular.module('biblioteca')
    .controller('site.list.controller', function ($scope, Site, toastr, $window, $state,  $q, $timeout) {

        $scope.sites = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingSites = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Site.getAll(function(data) {
                    $scope.sites = data;
                    $scope.loadingSites = false;
                }, function() {
                    toastr.error(Messages('app.error'), Messages('app.angular.error'));
                });

            });
            /*Fim - Promise*/

        };

        resolveLaterSites(defer);

        $scope.meuFiltro = function(site) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(site.titulo) || exp.test(site.pais.nome);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        $scope.abrirUrl = function($scope) {
            $window.open($scope, '_blank');
        };

        // carrega a pagina novamente
        $scope.reloadSites = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterSites(defer) {
          doLaterSites(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterSites(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
        }

    });
