# Biblioteca do Biogas / Biogas Library

### Idiomas
* Portugues, Espanhol e Ingles

### Version
* **2.0.0** [2020-07-28]
  * Initial release.
  
## Frameworks 
* **2.4.11** [Play Framework](https://playframework.com/) (Java) - Back End
* **1.6.4** [AngularJS](https://angularjs.org/) - Front End
* **0.13.13** [Sbt](http://www.scala-sbt.org/0.13/docs/Basic-Def.html) - Scala Build Tool Version

## Caracteristicas
* Dependencias sao gerenciadas pelo [Bower](https://bower.io/)
* Banco de dados [Postgresql](https://www.postgresql.org/)
* Internacionalizacao [i18n](https://www.w3.org/International/questions/qa-i18n)
* Realizado deploy em producao no [Apache Tomcat 8](https://tomcat.apache.org/download-80.cgi)

## To do
 - [X] Principais funcionalidades implementadas
 - [X] Funcionalidades de seguranca implementadas
 - [X] Procedimento de deploy em ambiente de teste via arquivo .war
 - [X] Procedimento de deploy em ambiente de producao via arquivo .war
 - [X] Procedimento de adicionar twitter widgets
 - [X] Procedimento de documentacao
 
## Ambiente de Producao
 - [https://biblioteca.cibiogas.org](https://biblioteca.cibiogas.org)
 
## Comandos deploy producao
 - Clone do projeto;
 - Realizar bower install na pasta raiz;
 - Verificar conexao com o banco;
 - Criar distro universal como servico -> sbt universal:package-zip-tarball;
 - Criar distro universal para deploy em apache tomcat -> sbt war.
 
## Fonte antigo
 - [https://github.com/haroldoramirez/biblioteca-antiga](https://github.com/haroldoramirez/biblioteca-antiga)
  
## Licenca
* PlayFramework - [Apache 2 license](https://www.apache.org/licenses/LICENSE-2.0.html)
* AngularJS - [MIT License](https://github.com/angular/angular.js/blob/master/LICENSE)

## Instituicao
Centro Internacional de Energias Renovaveis - Copyright 2020