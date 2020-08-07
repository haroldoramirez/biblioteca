angular.module('biblioteca')
    .controller('noticia.list.controller', function ($scope, Noticia, toastr, $window, $state, $q, $timeout) {

        $scope.filtro = '';
        $scope.noticias = [];
        let defer = $q.defer();
        $scope.loadingNoticias = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Noticia.getAll(function(data) {
                    $scope.noticias = data;
                    $scope.loadingNoticias = false;
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

        resolveLaterNoticias(defer);

        $scope.abrirUrl = function($scope) {
            $window.open($scope, '_blank');
        };

        $scope.meuFiltro = function(noticia) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(noticia.titulo);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        // carrega a pagina novamente
        $scope.reloadNoticias = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterNoticias(defer) {
          doLaterNoticias(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterNoticias(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
        }
        
    }).controller('noticia.detail.controller', function ($scope, $stateParams, $state, Noticia, toastr, $window, $q, $timeout) {

        $scope.noticia = {};
/*        let defer = $q.defer();
        $scope.loadingNoticia = true;*/

        $scope.init = function() {

            /*Inicio - Promise*/
           /* defer.promise.then(function() {*/

                $scope.noticia = Noticia.get({id:$stateParams.id}, function(data) {

                   $scope.loadingNoticia = false;

                }, function(data) {

                    switch (data.status) {
                        case 403:
                          $window.location.href = ('login');
                          break;
                        toastr.error(Messages('app.error'), Messages('app.angular.error'));
                    }

                });

         /*   });*/
            /*Fim - Promise*/

        };

/*        resolveLaterNoticia(defer);*/

        /*Resolve da Promise*/
/*        function resolveLaterNoticia(defer) {
          doLaterNoticia(defer, 'resolve');
        }*/

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
/*        function doLaterNoticia(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
        }*/
});