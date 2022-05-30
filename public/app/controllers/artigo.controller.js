angular.module('biblioteca')
    .controller('artigo.list.controller', function ($scope, Artigo, toastr, $window, $state, $q, $timeout) {

        $scope.artigos = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingArtigos = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Artigo.getAll(function(data) {
                    $scope.artigos = data;
                    $scope.loadingArtigos = false;
                }, function(data) {
                    switch (data.status) {
                        case 403:
                            $window.location.href = ('login');
                            break;
                        toastr.error(Messages('app.error'), Messages('app.angular.error'));
                    }
                });

            });
            /*Fim - Promise*/

        };

        resolveLaterArtigos(defer);

        $scope.meuFiltro = function(artigo) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(artigo.titulo) || exp.test(artigo.idioma.nome) || exp.test(artigo.autores) || exp.test(artigo.palavraChave);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        $scope.abrirLink = function($scope) {

            if ($scope.nomeArquivo === null || typeof $scope.nomeArquivo === "undefined" || $scope.nomeArquivo === 'arquivo.pdf' || $scope.nomeArquivo === "") {

                Artigo.getAcesso({titulo:$scope.titulo}, function(data) {
                    //Contabiliza o acesso quando existir link na nota tecnica
                }, function(data) {
                    //Caso ocorra um erro imprime no console do navegador
                    console.log(data);
                });

                $window.open($scope.url, '_blank');

            } else {
                $window.open('artigo/pdf/'+$scope.nomeArquivo, '_blank');
            }

        };

        // carrega a pagina novamente
        $scope.reloadArtigos = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterArtigos(defer) {
          doLaterArtigos(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterArtigos(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 900);
        }
});