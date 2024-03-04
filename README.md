# Biblioteca do Biogás / Biogas Library

### [![Build Status](https://travis-ci.org/haroldoramirez/biblioteca.svg?branch=master)](https://travis-ci.org/haroldoramirez/biblioteca)

## Projeto desenvolvido quando trabalhava no CIB.

### Projeto
* Repositório de arquivos institucionais como PDFs, Fotos, Projetos e atividades.
  Este repositório é apenas para consulta.

### Idiomas
* Português, Espanhol e Inglês

### Versão
* **1.0.0** [2017-05-04]
  * Initial release.
  
## Frameworks 
* **2.4.11** [Play Framework](https://playframework.com/) (Java) - Back End
* **1.6.4** [AngularJS](https://angularjs.org/) - Front End
* **0.13.13** [Sbt](http://www.scala-sbt.org/0.13/docs/Basic-Def.html) - Scala Build Tool Version

## Características
* Dependências são gerenciadas pelo [Bower](https://bower.io/)
* Banco de dados [Postgresql](https://www.postgresql.org/)
* Internacionalização [i18n](https://www.w3.org/International/questions/qa-i18n)
* Realizado deploy em produção no [Apache Tomcat® versão 8](https://tomcat.apache.org/download-80.cgi)

## To do
 - [X] Principais funcionalidades implementadas
 - [X] Funcionalidades de segurança implementadas
 - [X] Procedimento de deploy em ambiente de teste via arquivo .war
 - [X] Procedimento de deploy em ambiente de produção via arquivo .war
 - [X] Procedimento de adicionar twitter widgets
 - [X] Procedimento de documentação
 
## Ambiente de Produção
 - [https://biblioteca.cibiogas.org](https://biblioteca.cibiogas.org)
 
## Comandos deploy
 - Criar distro universal como serviço -> sbt universal:package-zip-tarball
 - Criar distro universal para deploy em apache tomcat -> sbt war
 
## Fonte antigo
 - [https://github.com/haroldoramirez/biblioteca-antiga](https://github.com/haroldoramirez/biblioteca-antiga)
  
## Licença
* PlayFramework - [Apache 2 license](https://www.apache.org/licenses/LICENSE-2.0.html)
* AngularJS - [MIT License](https://github.com/angular/angular.js/blob/master/LICENSE)

## Instituição
Desenvolvido por Haroldo Ramirez de Nobrega para o Centro Internacional de Energias Renováveis - Biogás - Copyright ® 2020
