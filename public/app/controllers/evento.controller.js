angular.module('biblioteca')
    .controller('evento.list.controller', function ($scope, Evento, toastr, $window, $state, $q, $timeout) {

        $scope.eventos = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingEventos = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Evento.getAll(function(data) {
                    $scope.eventos = data;

                    $scope.diaSemana = function( data ){
                        return moment(data).format('LLLL').split(',')[0];
                    };

                    $scope.mes = function( data ){
                        return moment(data).format('lll').split(' de ')[1];
                    };

                    $scope.dia = function( data ){
                        return moment(data).format('L').split('/')[0];
                    };

                    $scope.ano = function( data ){
                        return moment(data).format('L').split('/')[2];
                    };

                    $scope.loadingEventos = false;

                }, function() {
                    toastr.error(Messages('app.error'), Messages('app.angular.error'));
                });
            });
            /*Fim - Promise*/

        };

        resolveLaterEventos(defer);

        $scope.meuFiltro = function(evento) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(evento.nome) || exp.test(evento.localidade) || exp.test(evento.instituicao);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        $scope.abrirUrl = function($scope) {
            $window.open($scope, '_blank');
        };

        // carrega a pagina novamente
        $scope.reloadTodosEventos = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterEventos(defer) {
          doLaterEventos(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterEventos(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
        }

    }).controller('evento.futureList.controller', function ($scope, Evento, toastr, $window, $state, $q, $timeout) {

    $scope.eventosFuturos = [];
    $scope.filtro = '';
    let defer = $q.defer();
    $scope.loadingEventosFuturos = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Evento.getAll(function(data) {
                    $scope.eventosFuturos = data;

                    $scope.diaSemana = function( data ){
                        return moment(data).format('LLLL').split(',')[0];
                    };

                    $scope.mes = function( data ){
                        return moment(data).format('lll').split(' de ')[1];
                    };

                    $scope.dia = function( data ){
                        return moment(data).format('L').split('/')[0];
                    };

                    $scope.ano = function( data ){
                        return moment(data).format('L').split('/')[2];
                    };

                    $scope.loadingEventosFuturos = false;

                }, function() {
                    toastr.error(Messages('app.error'), Messages('app.angular.error'));
                });
            });
            /*Fim - Promise*/

        };

    resolveLaterEventosFuturos(defer);

    $scope.meuFiltro = function(evento) {
        var exp = new RegExp($scope.filtro, 'i');

        if($scope.filtro) {
            return exp.test(evento.nome) || exp.test(evento.localidade) || exp.test(evento.instituicao);
        }
        // se não digitou no filtro exibe tudo
        return true;
    };

    $scope.abrirUrl = function($scope) {
        $window.open($scope, '_blank');
    };

    // carrega a pagina novamente
    $scope.reloadEventosFuturos = function () {
        $state.go($state.current, {}, {reload: true});
    };

    /*Resolve da Promise*/
    function resolveLaterEventosFuturos(defer) {
      doLaterEventosFuturos(defer, 'resolve');
    }

    /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
    function doLaterEventosFuturos(defer, what) {
      $timeout(function() {
          defer[what]();
      }, 1000);
    }
});