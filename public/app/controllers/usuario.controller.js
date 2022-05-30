angular.module('biblioteca')
    .controller('usuario.perfil.controller', function ($scope, $rootScope, Usuario, toastr, ngDialog) {
        
    $scope.mostrar = false;

    $scope.init = function() {
      Usuario.getAutenticado(function(data) {
          $rootScope.usuario = data;
          $scope.mostrar = true;
       },function() {
            $scope.mostrar = false;
            toastr.error(Messages('app.error'), Messages('app.angular.error'));
         });
       };

       $scope.opendialog = function() {
           ngDialog.open({
               template: 'templateDialog',
               controller:'usuario.perfil.controller'
           });
       };

       $scope.sim = function() {
          Usuario.reset(function() {
            toastr.success(Messages('client.send.email.message'));
            $scope.closeThisDialog('Fechar');
            },function() {
              toastr.error(Messages('app.error'), Messages('app.angular.error'));
                $scope.closeThisDialog('Fechar');
            });
          };
  });