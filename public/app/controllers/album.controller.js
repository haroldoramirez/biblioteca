angular.module('biblioteca')
    .controller('album.list.controller', function ($scope, $state, Album, toastr, $window, $q, $timeout) {

        $scope.albuns = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingAlbuns = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Album.getAll(function(data) {
                    $scope.albuns = data;
                    $scope.loadingAlbuns = false;
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

        resolveLaterAlbuns(defer);

        // carrega a pagina novamente
        $scope.reloadAlbuns = function () {
            $state.go($state.current, {}, {reload: true});
        };

        $scope.meuFiltro = function(album) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(album.titulo);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        /*Resolve da Promise*/
        function resolveLaterAlbuns(defer) {
          doLaterAlbuns(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterAlbuns(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 900);
        }
    })
    .controller('album.detail.controller', function ($scope, $stateParams, $state, Album, toastr, $window, $q, $timeout) {

        $scope.album = {};
        let defer = $q.defer();
        $scope.loadingAlbum = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                $scope.album = Album.get({id:$stateParams.id}, function(data) {
                    $scope.loadingAlbum = false;
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

        resolveLaterAlbum(defer);

        /*Resolve da Promise*/
        function resolveLaterAlbum(defer) {
            doLaterAlbum(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterAlbum(defer, what) {
            $timeout(function() {
                defer[what]();
            }, 900);
        }

        // carrega a pagina novamente
        $scope.reloadAlbumFotos = function () {
            $state.go($state.current, {}, {reload: true});
        };

        $scope.meuFiltro = function(foto) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(foto.nome);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

});
