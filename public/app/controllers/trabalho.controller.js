angular.module('biblioteca')
    .controller('trabalho.list.controller', function ($scope, Trabalho, toastr, $window, $state, $q, $timeout) {

        $scope.trabalhos = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingTrabalhos = true;

        $scope.init = function() {
             /*Inicio - Promise*/
             defer.promise.then(function() {

                Trabalho.getAll(function(data) {
                    $scope.trabalhos = data;
                    $scope.loadingTrabalhos = false;
                }, function() {
                    toastr.error(Messages('app.error'), Messages('app.angular.error'));
                });
            });
            /*Fim - Promise*/
        };

        resolveLaterTrabalhos(defer);

        $scope.meuFiltro = function(trabalho) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(trabalho.titulo) || exp.test(trabalho.idioma.nome) || exp.test(trabalho.autores) || exp.test(trabalho.palavraChave);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        $scope.abrirLink = function($scope) {

            console.log($scope);

            if ($scope.nomeArquivo === undefined || $scope.nomeArquivo === 'arquivo.pdf' || $scope.nomeArquivo === '') {

                Trabalho.getAcesso({titulo:$scope.titulo}, function(data) {
                    //Contabiliza o acesso quando existir link na nota tecnica
                }, function(data) {
                    //Caso ocorra um erro imprime no console do navegador
                    console.log(data);
                });

                $window.open($scope.url, '_blank');
            } else {
                $window.open('trabalho/pdf/'+$scope.nomeArquivo, '_blank');
            }

        };

        // carrega a pagina novamente
        $scope.reloadTrabalhos = function () {
            $state.go($state.current, {}, {reload: true});
        };

          /*Resolve da Promise*/
          function resolveLaterTrabalhos(defer) {
              doLaterTrabalhos(defer, 'resolve');
          }

          /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
          function doLaterTrabalhos(defer, what) {
              $timeout(function() {
                  defer[what]();
              }, 900);
          }
    });