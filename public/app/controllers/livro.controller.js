angular.module('biblioteca')
    .controller('livro.list.controller', function ($scope, Livro, toastr) {
    $scope.init = function() {
        $scope.nomeFiltro = '';
        $scope.filtrados = 0;

        Livro.getAll(function(data) {
            $scope.livros = data;
            $scope.quantidade = $scope.livros.length;
        }, function() {
            toastr.error(Messages('app.error'), Messages('app.angular.error'));
        });
    };
});