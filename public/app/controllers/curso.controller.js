angular.module('biblioteca')
  .controller('curso.list.controller', function ($scope, Curso, toastr, $window, $state, $q, $timeout) {

      $scope.cursos = [];
      $scope.filtro = '';
      let defer = $q.defer();
      $scope.loadingCursos = true;

      /*Inicia quando carrega a pagina*/
      $scope.init = function() {

         /*Inicio - Promise*/
         defer.promise.then(function() {

            Curso.getAll(function(data) {
                $scope.cursos = data;
                $scope.loadingCursos = false;
            }, function() {
                toastr.error(Messages('app.error'), Messages('app.angular.error'));
            });
        });
        /*Fim - Promise*/

      };

      resolveLaterCursos(defer);

      $scope.abrirUrl = function($scope) {
          $window.open($scope, '_blank');
      };

      $scope.meuFiltro = function(curso) {
          var exp = new RegExp($scope.filtro, 'i');

          if($scope.filtro) {
              return exp.test(curso.nome);
          }
          // se não digitou no filtro exibe tudo
          return true;
      };

      // carrega a pagina novamente
      $scope.reloadCursos = function () {
          $state.go($state.current, {}, {reload: true});
      };

      /*Resolve da Promise*/
      function resolveLaterCursos(defer) {
          doLaterCursos(defer, 'resolve');
      }

      /*Funcao que e executada em um delay - para auxiliar nas animacoes*/
      function doLaterCursos(defer, what) {
          $timeout(function() {
              defer[what]();
          }, 1000);
      }
});