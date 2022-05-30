angular.module('biblioteca')
    .controller('video.list.controller', function ($scope, Video, toastr, $window, $state, $q, $timeout) {

        $scope.videos = [];
        $scope.filtro = '';
        let defer = $q.defer();
        $scope.loadingVideos = true;

        $scope.init = function() {

            /*Inicio - Promise*/
            defer.promise.then(function() {

                Video.getAll(function(data) {
                    $scope.videos = data;
                    $scope.loadingVideos = false;
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

        resolveLaterVideos(defer);

        $scope.meuFiltro = function(video) {
            var exp = new RegExp($scope.filtro, 'i');

            if($scope.filtro) {
                return exp.test(video.titulo);
            }
            // se não digitou no filtro exibe tudo
            return true;
        };

        // carrega a pagina videos novamente
        $scope.reloadVideos = function () {
            $state.go($state.current, {}, {reload: true});
        };

        /*Resolve da Promise*/
        function resolveLaterVideos(defer) {
          doLaterVideos(defer, 'resolve');
        }

        /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
        function doLaterVideos(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
        }
});