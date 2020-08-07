angular.module('biblioteca')
    .service('Usuario',['$resource',
      function($resource){
        return $resource('usuario/:id', {}, {
            cadastrar: {method: 'POST', url: 'usuario/cadastrar', isArray: false},
            update: {method: 'PUT', url: 'usuario/:id', isArray: false},
            getAll: {method: 'GET', url: 'usuarios', isArray: true},
            reset: {method: 'POST', url: 'reset/senha', isArray: false},
            getFiltroUsuarios: {method: 'GET', url: 'usuarios/filtro/:filtro', isArray: true},
            getAutenticado: {method: 'GET', url: 'current', isArray: false}
        });
    }]).service('Contato',['$resource',
      function($resource){
        return $resource('contato/:id', {}, {
            getAll: {method: 'GET', url: 'contatos', isArray: true},
            getFiltroContatos: {method: 'GET', url: 'contatos/filtro/:filtro', isArray: true}
        });
    }]).service('Artigo',['$resource',
      function($resource){
        return $resource('artigo/:id', {}, {
            getAll: {method: 'GET', url: 'artigos', isArray: true},
            getFiltroArtigos: {method: 'GET', url: 'artigos/filtro/:filtro', isArray: true},
            getAcesso: {method: 'GET', url: 'artigo/link/:titulo', isArray: false}
        });
    }]).service('Trabalho',['$resource',
    function($resource){
        return $resource('trabalho/:id', {}, {
            getAll: {method: 'GET', url: 'trabalhos', isArray: true},
            getFiltroArtigos: {method: 'GET', url: 'trabalhos/filtro/:filtro', isArray: true},
            getAcesso: {method: 'GET', url: 'trabalho/link/:titulo', isArray: false}
        });
    }]).service('Livro',['$resource',
      function($resource){
        return $resource('livro/:id', {}, {
            getAll: {method: 'GET', url: 'livros', isArray: true},
            getFiltroLivros: {method: 'GET', url: 'livros/filtro/:filtro', isArray: true}
        });
    }]).service('Evento',['$resource',
        function($resource){
            return $resource('evento/:id', {}, {
                getAll: {method: 'GET', url: 'eventos', isArray: true},
                getAllFutureEvents: {method: 'GET', url: 'eventos/futuros', isArray: true},
                getFiltroEventos: {method: 'GET', url: 'eventos/filtro/:filtro', isArray: true},
                getLast: {method: 'GET', url: 'eventos/futuros', isArray: true}
            });
    }]).service('Video',['$resource',
        function($resource){
            return $resource('video/:id', {}, {
                getAll: {method: 'GET', url: 'videos', isArray: true}
            });
    }]).service('Curso',['$resource',
        function($resource){
            return $resource('curso/:id', {}, {
                getAll: {method: 'GET', url: 'cursos', isArray: true}
            });
    }]).service('Noticia',['$resource',
        function($resource){
          return $resource('noticia/detalhe/:id', {}, {
                getAll: {method: 'GET', url: 'noticias', isArray: true},
                getLast: {method: 'GET', url: 'noticias/ultimas', isArray: true}
            });
    }]).service('Publicacao',['$resource',
        function($resource){
            return $resource('publicacao/detalhe/:id', {}, {
                getAll: {method: 'GET', url: 'publicacoes', isArray: true},
                getLast: {method: 'GET', url: 'publicacoes/ultimas', isArray: true},
                getAcesso: {method: 'GET', url: 'publicacao/link/:titulo', isArray: false}
            });
    }]).service('NotaTecnica',['$resource',
        function($resource){
            return $resource('notatecnica/detalhe/:id', {}, {
                getAll: {method: 'GET', url: 'notastecnicas', isArray: true},
                getLast: {method: 'GET', url: 'notastecnicas/ultimas', isArray: true},
                getAcesso: {method: 'GET', url: 'notatecnica/link/:titulo', isArray: false}
            });
    }]).service('Marco',['$resource',
        function($resource){
            return $resource('marco/:id', {}, {
                getAll: {method: 'GET', url: 'marcos', isArray: true}
            });
    }]).service('Album',['$resource',
        function($resource){
            return $resource('album/:id', {}, {
                getAll: {method: 'GET', url: 'albuns', isArray: true}
            });
    }]).service('Site',['$resource',
        function($resource){
            return $resource('site/:id', {}, {
                getAll: {method: 'GET', url: 'sites', isArray: true}
        });
    }]).service('Avaliacao',['$resource',
        function($resource){
            return $resource('avaliacao/:id', {}, {
                getAll: {method: 'GET', url: 'avaliacoes', isArray: true}
        });
    }]).service('Imagem',['$resource',
        function($resource){
            return $resource('imagem/:id', {}, {
                getAll: {method: 'GET', url: 'imagens', isArray: true}
            });
    }]);