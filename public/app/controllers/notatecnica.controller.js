angular.module('biblioteca')
    .controller('notatecnica.list.controller', function ($scope, NotaTecnica, toastr, $window, $state, $q, $timeout) {

        $scope.notasTecnicas = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingNotasTecnicas = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                NotaTecnica.getAll(function(data) {
                    $scope.notasTecnicas = data;
                    $scope.loadingNotasTecnicas = false;
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

        resolveLaterNotasTecnicas(defer);

        $scope.meuFiltro = function(notatecnica) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(notatecnica.titulo) || exp.test(notatecnica.idioma.nome) || exp.test(notatecnica.autor) || exp.test(notatecnica.ano) || exp.test(notatecnica.palavraChave);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        // carrega a pagina novamente
        $scope.reloadNotasTecnicas = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterNotasTecnicas(defer) {
            doLaterNotasTecnicas(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterNotasTecnicas(defer, what) {
            $timeout(function() {
                defer[what]();
            }, 900);
        }

    }).controller('notatecnica.detail.controller', function ($scope, $stateParams, $state, NotaTecnica, toastr, $window, $q, $timeout) {

    $scope.notaTecnica = {};
    let defer = $q.defer();
    $scope.loadingNotaTecnica = true;

    $scope.init = function() {

        /*Inicio - Promise*/
        defer.promise.then(function() {

            $scope.notaTecnica = NotaTecnica.get({id:$stateParams.id}, function(data) {
                $scope.loadingNotaTecnica = false;

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

    resolveLaterNotaTecnica(defer);

    /*Resolve da Promise*/
    function resolveLaterNotaTecnica(defer) {
        doLaterNotaTecnica(defer, 'resolve');
    }

    /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
    function doLaterNotaTecnica(defer, what) {
        $timeout(function() {
            defer[what]();
        }, 800);
    }

    $scope.abrirLink = function($scope) {

        console.log($scope);

        if ($scope.nomeArquivo === undefined || $scope.nomeArquivo === 'arquivo.pdf' || $scope.nomeArquivo === '') {

            NotaTecnica.getAcesso({titulo:$scope.titulo}, function(data) {
                //Contabiliza o acesso quando existir link na nota tecnica
            }, function(data) {
                //Caso ocorra um erro imprime no console do navegador
               console.log(data);
            });

            $window.open($scope.url, '_blank');

        } else {
            $window.open('notatecnica/pdf/'+$scope.nomeArquivo, '_blank');
        }

    };
});