angular.module('biblioteca')
    .controller('publicacao.list.controller', function ($scope, Publicacao, toastr, $window, $state, $q, $timeout) {

        $scope.publicacoes = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingPublicacoes = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Publicacao.getAll(function(data) {
                    $scope.publicacoes = data;
                    $scope.loadingPublicacoes = false;
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

        $scope.loadMore = function() {
            var last = $scope.publicacoes[$scope.publicacoes.length - 1];
            for(var i = 1; i <= 8; i++) {
              $scope.publicacoes.push(last + i);
            }
          };

        resolveLaterPublicacoes(defer);

        $scope.meuFiltro = function(publicacao) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(publicacao.titulo) || exp.test(publicacao.idioma.nome) || exp.test(publicacao.autor) || exp.test(publicacao.ano) || exp.test(publicacao.palavraChave);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        // carrega a pagina novamente
        $scope.reloadPublicacoes = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterPublicacoes(defer) {
          doLaterPublicacoes(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterPublicacoes(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 900);
        }

    }).controller('publicacao.detail.controller', function ($scope, $stateParams, $state, Publicacao, toastr, $window, $q, $timeout) {

        $scope.publicacao = {};
        let defer = $q.defer();
        $scope.loadingPublicacao = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                $scope.publicacao = Publicacao.get({id:$stateParams.id}, function(data) {
                    $scope.loadingPublicacao = false;

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

        resolveLaterPublicacao(defer);

        /*Resolve da Promise*/
        function resolveLaterPublicacao(defer) {
            doLaterPublicacao(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterPublicacao(defer, what) {
            $timeout(function() {
                defer[what]();
            }, 800);
        }

        $scope.abrirLink = function($scope) {

            if ($scope.nomeArquivo === null || typeof $scope.nomeArquivo === "undefined" || $scope.nomeArquivo === 'arquivo.pdf' || $scope.nomeArquivo === "") {

                Publicacao.getAcesso({titulo:$scope.titulo}, function(data) {
                    //Contabiliza o acesso quando existir link na nota tecnica
                }, function(data) {
                    //Caso ocorra um erro imprime no console do navegador
                    console.log(data);
                });

                $window.open($scope.url, '_blank');

            } else {
                $window.open('publicacao/pdf/'+$scope.nomeArquivo, '_blank');
            }

        };
});
