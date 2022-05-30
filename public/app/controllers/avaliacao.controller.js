angular.module('biblioteca')
    .controller('avaliacao.controller', function ($scope, Avaliacao, toastr, $state, ngDialog, Upload) {

        $scope.opendialogtermo = function() {
            ngDialog.open({
                template: 'dialogTermo',
                controller:'avaliacao.controller',
                width: 1000
            });
        };

        $scope.uploadPdf = function(arquivoPdf) {
            var uploadRequest = Upload.upload({
                url: 'avaliacao',
                data: {
                    nome: $scope.avaliacao.nome,
                    email: $scope.avaliacao.email,
                    telefone: $scope.avaliacao.telefone,
                    cpf: $scope.avaliacao.cpf,
                    rg: $scope.avaliacao.rg,
                    urlLattes: $scope.avaliacao.urlLattes,
                    titulo: $scope.avaliacao.titulo,
                    outrosAutores: $scope.avaliacao.outrosAutores,
                    urlDocumento: $scope.avaliacao.urlDocumento,
                    termo: $scope.avaliacao.termo,
                    mensagem: $scope.avaliacao.mensagem,
                    file: arquivoPdf
                }
            });

            uploadRequest.then(function () {
                toastr.success(Messages('Informações enviadas com sucesso'));
                $state.reload();
            }, function (response) {
                toastr.error(response.data, Messages('app.angular.error'));
            });
        };

    });
