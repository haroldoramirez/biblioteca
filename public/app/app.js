angular
    .module
        ('biblioteca',
            ['ui.router',
             'ngResource',
             'angular-loading-bar',
             'toastr',
             'ngDialog',
             'angular-carousel',
             'ngFileUpload',
             'ngAnimate',
             'angular-google-analytics',
             'ncy-angular-breadcrumb',
             'ngMeta'
            ]
        )
    //Rotas da aplicacao
    .config(function ($stateProvider, $urlRouterProvider, $locationProvider) {

        $locationProvider.html5Mode({enabled: false, requireBase: false, rewriteLinks: false});

        $urlRouterProvider.otherwise("/");

        $stateProvider
            .state('home', {
                url: "/",
                templateUrl: 'assets/app/views/home.html',
                controller: 'home.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.1')
                  },
                activetab: 'home',
                data: {
                    meta: {
                        'og:image': 'https://biblioteca.cibiogas.org/biblioteca/assets/images/cibiogasicon.png',
                        'author': 'Centro Internacional de Energias Renováveis',
                        'title': 'Bem vindo',
                        'og:title': 'Tudo sobre Biogás',
                        'og:description': 'O espaço tem como objetivo organizar, compartilhar e promover a troca de conhecimento na área do biogás, voltado para o enriquecimento de pesquisas nacionais e internacionais.'
                    }
                }
            })
            .state('teste', {
                url: "/teste",
                templateUrl: 'assets/app/views/teste.html',
                controller: 'teste.controller',
                activetab: 'teste'
            })
            .state('membro', {
                url: "/membro",
                templateUrl: 'assets/app/views/membro.html',
                controller: 'home.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.top.title.18')
                  },
                activetab: 'home',
                data: {
                    meta: {
                        'title': 'Membro',
                        'description': 'O registro é gratuito. Quando você preencher todos os campos solicitados e registrar-se, você receberá um e-mail para ativar sua conta.'
                    }
                }
            })
            .state('termo', {
                url: "/termo",
                templateUrl: 'assets/app/views/termo.html',
                controller: 'home.controller',
                activetab: 'home'
            })
            .state('compartilhar', {
                url: "/compartilhar",
                templateUrl: 'assets/app/views/avaliacoes/create.html',
                controller: 'avaliacao.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.top.title.19')
                  },
                activetab: 'compartilhar',
                data: {
                    meta: {
                        'title': 'Compartilhar',
                        'description': 'As produções aqui disponibilizadas não refletem necessariamente a opinião ou posicionamento científico do CIBiogás ou da equipe da Biblioteca Digital. Buscamos centralizar conhecimentos de biogás com o intuito de fomentar o uso desse recurso.'
                    }
                }
            })
            .state('contato', {
                url: "/contato",
                templateUrl: 'assets/app/views/contatos/create.html',
                controller: 'contato.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.top.title.20')
                  },
                activetab: 'contato',
                data: {
                    meta: {
                        'title': 'Entre em contato',
                        'description': 'A sua opinião é muito importante para nós.'
                    }
                }
            })
            .state('biogas', {
                url: "/biogas",
                templateUrl: 'assets/app/views/biogas.html',
                controller: 'home.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.1')
                  },
                activetab: 'biogas',
                data: {
                    meta: {
                        'title': 'O que é biogas?',
                        'description': 'Com o biogás é possível gerar energias elétrica e térmica, além de biocombustível (biometano). Durante o processo, também há produção de biofertilizante. Por isso, a produção de biogás tem sido apresentada por especialistas como a melhor alternativa para oferecer condições ideais de expansão e competitividade para o agronegócio brasileiro.'
                    }
                }
            })
            .state('busca', {
                url: "/busca",
                templateUrl: 'assets/app/views/busca.html',
                controller: 'busca.controller',
                  ncyBreadcrumb: {
                    label: Messages('search.page.title')
                  },
            })
            .state('usuarios', {
                url: "/usuario/perfil",
                templateUrl: 'assets/app/views/usuarios/perfil.html',
                controller: 'usuario.perfil.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.top.title.17')
                  },
            })
            .state('livros', {
                url: "/livros",
                templateUrl: 'assets/app/views/livros/list.html',
                controller: 'livro.list.controller'
            })
            .state('cursos', {
                url: "/cursos",
                templateUrl: 'assets/app/views/cursos/list.html',
                controller: 'curso.list.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.2')
                  },
                data: {
                    meta: {
                        'title': 'Cursos',
                        'description': 'Nosso conhecimento é produzido por especialistas que atuam na área, o conhecimento é diretamente relacionado a prática.'
                    }
                }
            })
            .state('artigos', {
                url: "/artigos",
                templateUrl: 'assets/app/views/artigos/list.html',
                controller: 'artigo.list.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.4')
                },
                data: {
                    meta: {
                        'title': 'Artigos',
                        'description': 'Artigos sobre Biogás você encontra aqui.'
                    }
                }
            })
            .state('trabalhos', {
                url: "/trabalhos",
                templateUrl: 'assets/app/views/trabalhos/list.html',
                controller: 'trabalho.list.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.17')
                },
                data: {
                    meta: {
                        'title': 'Trabalhos Acadêmicos',
                        'description': 'Trabalhos sobre Biogás você encontra aqui.'
                    }
                }
            })
            .state('videos', {
                url: "/videos",
                templateUrl: 'assets/app/views/videos/list.html',
                controller: 'video.list.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.6')
                },
                data: {
                    meta: {
                        'title': 'Vídeos',
                        'description': 'Vídeos sobre Biogás você encontra aqui.'
                    }
                }
            })
            .state('marcos', {
                url: "/marcos",
                templateUrl: 'assets/app/views/marcos/list.html',
                controller: 'marcos.list.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.biogas.6')
                },
                data: {
                    meta: {
                        'title': 'Marcos Regulatórios',
                        'description': 'Marcos Regulatórios sobre Biogás você encontra aqui.'
                    }
                }
            })
            .state('eventos/todos', {
                url: "/eventos/todos",
                templateUrl: 'assets/app/views/eventos/list.html',
                controller: 'evento.list.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.9')
                  },
                activetab: 'eventos',
                data: {
                    meta: {
                        'title': 'Eventos',
                        'description': 'Todos os eventos sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('eventos/futuros', {
                url: "/eventos/futuros",
                templateUrl: 'assets/app/views/eventos/futureList.html',
                controller: 'evento.futureList.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.biogas.9')
                },
                activetab: 'eventos',
                data: {
                    meta: {
                        'title': 'Eventos',
                        'description': 'Eventos sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('noticias', {
                url: "/noticias",
                templateUrl: 'assets/app/views/noticias/list.html',
                controller: 'noticia.list.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.10')
                  },
                activetab: 'noticias',
                data: {
                    meta: {
                        'title': 'Notícias',
                        'description': 'Notícias sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('noticias/detalhe', {
                url: "/noticias/detalhe/:id",
                templateUrl: 'assets/app/views/noticias/detail.html',
                controller: 'noticia.detail.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.10')
                  },
                activetab: 'noticias',
                data: {
                    meta: {
                        'title': 'Notícias',
                        'description': 'Notícias sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('publicacoes', {
                url: "/publicacoes",
                templateUrl: 'assets/app/views/publicacoes/list.html',
                controller: 'publicacao.list.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.11')
                  },
                activetab: 'publicacoes',
                data: {
                    meta: {
                        'title': 'Publicações',
                        'description': 'Publicações sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('publicacoes/detalhe', {
                url: "/publicacoes/detalhe/:id",
                templateUrl: 'assets/app/views/publicacoes/detail.html',
                controller: 'publicacao.detail.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.11')
                  },
                activetab: 'publicacoes',
                data: {
                    meta: {
                        'title': 'Publicações',
                        'description': 'Publicações sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('notastecnicas', {
                url: "/notastecnicas",
                templateUrl: 'assets/app/views/notastecnicas/list.html',
                controller: 'notatecnica.list.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.19')
                },
                activetab: 'notastecnicas',
                data: {
                    meta: {
                        'title': 'Notas técnicas',
                        'description': 'Notas técnicas sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('notastecnicas/detalhe', {
                url: "/notastecnicas/detalhe/:id",
                templateUrl: 'assets/app/views/notastecnicas/detail.html',
                controller: 'notatecnica.detail.controller',
                ncyBreadcrumb: {
                    label: Messages('menu.19')
                },
                activetab: 'notastecnicas',
                data: {
                    meta: {
                        'title': 'Notas técnicas',
                        'description': 'Notas técnicas sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('albuns', {
                url: "/albuns",
                templateUrl: 'assets/app/views/albuns/list.html',
                controller: 'album.list.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.7')
                  },
                activetab: 'albuns',
                data: {
                    meta: {
                        'title': 'Fotos',
                        'description': 'Fotos sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('albuns/detalhe', {
                url: "/albuns/detalhe/:id",
                templateUrl: 'assets/app/views/albuns/detail.html',
                controller: 'album.detail.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.7')
                  },
                activetab: 'albuns',
                data: {
                    meta: {
                        'title': 'Albuns',
                        'description': 'Fotos sobre biogás você encontra aqui.'
                    }
                }
            })
            .state('sites', {
                url: "/sites",
                templateUrl: 'assets/app/views/sites/list.html',
                controller: 'site.list.controller',
                  ncyBreadcrumb: {
                    label: Messages('menu.biogas.12')
                  },
                data: {
                    meta: {
                        'title': 'Sites',
                        'description': 'Portal sobre biogás você encontra aqui.'
                    }
                }
            })
    //Removar o circulo de carregamento da loading bar
    }).config(function($httpProvider, cfpLoadingBarProvider) {
        cfpLoadingBarProvider.includeSpinner = false;
    //Configuracoes da torrada - toastr
    }).config(function(toastrConfig) {
        angular.extend(toastrConfig, {
            positionClass: 'toast-bottom-right',
            allowHtml: false,
            closeButton: true,
            closeHtml: '<button>&times;</button>',
            extendedTimeOut: 2000,
            iconClasses: {
                error: 'toast-error',
                info: 'toast-info',
                success: 'toast-success',
                warning: 'toast-warning'
            },
            messageClass: 'toast-message',
            onHidden: null,
            onShown: null,
            onTap: null,
            progressBar: false,
            tapToDismiss: true,
            templates: {
                toast: 'directives/toast/toast.html',
                progressbar: 'directives/progressbar/progressbar.html'
            },
            timeOut: 7000,
            titleClass: 'toast-title',
            toastClass: 'toast'
       });
    }).run(function ($rootScope) {
        $rootScope.$on('$viewContentLoaded', function upgradeAllRegistered() {
            componentHandler.upgradeAllRegistered();
        });
    //Recebe as mensagens internacionalizadas do backend
    }).run(function ($rootScope) {
        $rootScope.Messages = window.Messages;
    }).run(function($rootScope, $state) {
        $rootScope.$state = $state;
    //diretiva de clickar uma vez nos botoes
    }).directive('clickOnce', function($timeout) {
        return {
            restrict: 'A',
            link: function(scope, element, attrs) {
                var replacementText = attrs.clickOnce;
                element.bind('click', function() {
                    $timeout(function() {
                        if (replacementText) {
                            element.html(replacementText);
                        }
                        element.attr('disabled', true);
                    }, 0);
                });
            }
        };
    //Configuracoes de dialog do sistema
    }).config(['ngDialogProvider', function (ngDialogProvider) {
        ngDialogProvider.setDefaults({
            className: 'ngdialog-theme-default',
            showClose: true,
            closeByDocument: true,
            closeByEscape: true
        });
    //Codigo do google analytics
    }]).config(['AnalyticsProvider', function (AnalyticsProvider) {
        // Add configuration code as desired
        AnalyticsProvider.setAccount('UA-59645436-3');  //UA-59645436-3 should be your tracking code
    //Breadcrumbs do menu superior - altera o titulo da pagina quando modificado a state
    }]).run(['Analytics', function(Analytics) { }])
        .config(function($breadcrumbProvider) {
        $breadcrumbProvider.setOptions({
         template: '<div><span ng-repeat="step in steps">{{step.ncyBreadcrumbLabel}}</span></div>'
        });
    })
    /*Configuracao de loading dos cards*/
    .config(function($provide) {
        $provide.decorator('$q', ['$delegate', '$rootScope', function($delegate, $rootScope) {
            var pendingPromisses = 0;
            $rootScope.$watch(
                function() { return pendingPromisses > 0; },
                function(loading) { $rootScope.loading = loading; }
            );
            var $q = $delegate;
            var origDefer = $q.defer;
            $q.defer = function() {
                var defer = origDefer();
                pendingPromisses++;
                defer.promise.finally(function() {
                  pendingPromisses--;
                });
                return defer;
            };
            return $q;
        }])
    })
    /*Configuracao necessaria para remover o erro Possibly unhandled rejection: canceled*/
    .config(function ($qProvider) {
          $qProvider.errorOnUnhandledRejections(false);
    })
    /*Cache-busting strategy*/
    .config(['$httpProvider', function($httpProvider) {
        var __version_number = 6.0; // cacheBustSuffix = Date.now('U'); // 'U' -> linux/unix epoch date int
        $httpProvider.interceptors.push(function () {
            return {
                'request': function (config) {
                    // !!config.cached represents if the request is resolved using
                    // the angular-templatecache
                    if (!config.cached) {
                        config.url += ((config.url.indexOf('?')>-1)?'&':'?') + config.paramSerializer({v: __version_number});
                    } else if (config.url.indexOf('no-cache') > -1) {
                        // if the cached URL contains 'no-cache' then remove it from the cache
                        config.cache.remove(config.url);
                        config.cached = false; // unknown consequences
                        // Warning: if you remove the value form the cache, and the asset is not
                        // accessable at the given URL, you will get a 404 error.
                    }
                    return config;
                }
            }
        });
    }])
    //Configuracoes sobre as metatags
    .config(function(ngMetaProvider) {
        ngMetaProvider.useTitleSuffix(true);
        ngMetaProvider.setDefaultTitle('Biogas');
        ngMetaProvider.setDefaultTitleSuffix(' | Biblioteca do Biogas');
        ngMetaProvider.setDefaultTag('author', 'Centro Internacional de Energias Renováveis');
    })
    //Iniciar meta tags
    .run(function(ngMeta) {
        ngMeta.init();
    }).directive('fadeIn', function($timeout){
          return {
              restrict: 'A',
              link: function($scope, $element, attrs){
                  $element.addClass("ng-hide-remove");
                  $element.on('load', function() {
                      $element.addClass("ng-hide-add");
                  });
              }
          };
      });
