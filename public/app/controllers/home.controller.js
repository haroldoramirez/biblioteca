angular.module('biblioteca')
  .controller('home.controller', function ($scope, Contato, toastr, $state, Carousel, Publicacao, NotaTecnica, Noticia, Evento, Imagem, $q, $timeout) {

      let slidesCount = 0;
      $scope.Carousel = {};
      $scope.Carousel = Carousel;
      $scope.imagens = [];
      $scope.publicacoes = [];
      $scope.noticias = [];
      $scope.eventos = [];

      $scope.inicioCarousel = function() {

          Imagem.getAll(function(data) {
              $scope.imagens = data;

          }, function() {
              toastr.error(Messages('app.error'), Messages('app.angular.error'));
          });

      };

      $scope.anterior = function() {
          Carousel.get('carousel-home').previous();
      };

      $scope.proximo = function() {
          Carousel.get('carousel-home').next();
      };

      $scope.inicioNotastecnicas = function() {
          NotaTecnica.getLast(function(data) {
              $scope.notasTecnicas = data;
          }, function() {
              toastr.error(Messages('app.error'), Messages('app.angular.error'));
          });
      };

      $scope.inicioPublicacoes = function() {
          Publicacao.getLast(function(data) {
              $scope.publicacoes = data;
          }, function() {
              toastr.error(Messages('app.error'), Messages('app.angular.error'));
          });
      };

      $scope.inicioNoticias = function() {
          Noticia.getLast(function(data) {
              $scope.noticias = data;
          }, function() {
              toastr.error(Messages('app.error'), Messages('app.angular.error'));
          });
      };

      $scope.inicioEventos = function() {
          Evento.getLast(function(data) {
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

          }, function() {
              toastr.error(Messages('app.error'), Messages('app.angular.error'));
          });

      };

});