angular.module('biblioteca')
    .controller('marcos.list.controller', function ($scope, Marco, toastr, $window, $state, $q, $timeout) {

        $scope.marcos = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingMarcos = true;

        $scope.init = function() {


            /*Inicio - Promise*/
            defer.promise.then(function() {

                Marco.getAll(function(data) {
                    $scope.marcos = data;
                      $scope.loadingMarcos = false;
                }, function() {
                    toastr.error(Messages('app.error'), Messages('app.angular.error'));
                });

            });
            /*Fim - Promise*/

        };

        resolveLaterMarcos(defer);

        $scope.meuFiltro = function(marco) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(marco.titulo) || exp.test(marco.ambito) || exp.test(marco.responsavel) || exp.test(marco.ano) || exp.test(marco.categoria.nome);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        $scope.abrirUrl = function($scope) {
            $window.open($scope, '_blank');
        };

        // carrega a pagina novamente
        $scope.reloadMarcos = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterMarcos(defer) {
          doLaterMarcos(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterMarcos(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
        }

    });
