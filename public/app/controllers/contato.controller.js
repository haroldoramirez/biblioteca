angular.module('biblioteca')
    .controller('contato.controller', function ($scope, Contato, toastr, $state) {

        $scope.save = function() {
            Contato.save($scope.contato, function(data) {
                toastr.success(Messages('client.send.email.message'));
                $state.reload();
            }, function() {
                toastr.error(Messages('app.error'), Messages('app.angular.error'));
            });
        };

    });