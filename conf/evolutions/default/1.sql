# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table album (
  id                            bigserial not null,
  titulo                        varchar(50) not null,
  descricao                     varchar(500) not null,
  nome_capa                     varchar(250) not null,
  nome_pasta                    varchar(250) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  constraint pk_album primary key (id)
);

create table artigo (
  id                            bigserial not null,
  titulo                        varchar(350) not null,
  resumo                        varchar(600) not null,
  nome_arquivo                  varchar(400) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  numero_acesso                 integer not null,
  idioma_id                     bigint,
  autores                       varchar(500) not null,
  palavra_chave                 varchar(200) not null,
  url                           varchar(500),
  constraint pk_artigo primary key (id)
);

create table avaliacao (
  id                            bigserial not null,
  nome                          varchar(200) not null,
  email                         varchar(150) not null,
  telefone                      varchar(18) not null,
  cpf                           varchar(20) not null,
  rg                            varchar(18) not null,
  url_lattes                    varchar(450),
  titulo                        varchar(400) not null,
  outros_autores                varchar(600),
  url_documento                 varchar(450),
  termo                         boolean not null,
  mensagem                      varchar(400),
  nome_arquivo                  varchar(450),
  status                        varchar(9),
  data_cadastro                 date not null,
  constraint ck_avaliacao_status check (status in ('APROVADO','REPROVADO','AVALIAR')),
  constraint pk_avaliacao primary key (id)
);

create table categoria (
  id                            bigserial not null,
  nome                          varchar(100) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  constraint pk_categoria primary key (id)
);

create table contato (
  id                            bigserial not null,
  nome                          varchar(80) not null,
  email                         varchar(80) not null,
  assunto                       varchar(50) not null,
  mensagem                      varchar(400) not null,
  data_cadastro                 date not null,
  constraint pk_contato primary key (id)
);

create table foto (
  id                            bigserial not null,
  album_id                      bigint not null,
  nome                          varchar(150) not null,
  descricao                     varchar(400),
  nome_arquivo                  varchar(250) not null,
  data_alteracao                date,
  constraint pk_foto primary key (id)
);

create table home (
  id                            bigserial not null,
  descricao                     varchar(400) not null,
  url                           varchar(600),
  data_cadastro                 date not null,
  data_alteracao                date,
  nome_arquivo                  varchar(400) not null,
  constraint pk_home primary key (id)
);

create table idioma (
  id                            bigserial not null,
  nome                          varchar(200) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  constraint uq_idioma_nome unique (nome),
  constraint pk_idioma primary key (id)
);

create table log (
  id                            bigserial not null,
  mensagem                      varchar(500) not null,
  navegador                     varchar(100),
  versao                        varchar(100),
  so                            varchar(100),
  data_cadastro                 timestamp not null,
  constraint pk_log primary key (id)
);

create table marco (
  id                            bigserial not null,
  titulo                        varchar(350) not null,
  ambito                        varchar(50) not null,
  responsavel                   varchar(100) not null,
  ano                           varchar(15) not null,
  url                           varchar(400) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  nome_capa                     varchar(400) not null,
  categoria_id                  bigint,
  constraint pk_marco primary key (id)
);

create table nota_tecnica (
  id                            bigserial not null,
  titulo                        varchar(350) not null,
  resumo                        varchar(600) not null,
  url                           varchar(400),
  idioma_id                     bigint,
  autor                         varchar(500) not null,
  ano                           varchar(15) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  nome_capa                     varchar(400) not null,
  numero_acesso                 integer,
  palavra_chave                 varchar(200) not null,
  nome_arquivo                  varchar(400) not null,
  constraint pk_nota_tecnica primary key (id)
);

create table pais (
  id                            bigserial not null,
  nome                          varchar(200) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  constraint uq_pais_nome unique (nome),
  constraint pk_pais primary key (id)
);

create table publicacao (
  id                            bigserial not null,
  titulo                        varchar(350) not null,
  resumo                        varchar(600) not null,
  url                           varchar(400),
  idioma_id                     bigint,
  autor                         varchar(500) not null,
  ano                           varchar(15) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  nome_capa                     varchar(400) not null,
  numero_acesso                 integer,
  palavra_chave                 varchar(200) not null,
  nome_arquivo                  varchar(400) not null,
  constraint pk_publicacao primary key (id)
);

create table token (
  token                         varchar(255) not null,
  usuario_id                    bigint,
  type                          varchar(8),
  date_creation                 date,
  email                         varchar(255),
  constraint ck_token_type check (type in ('password','email')),
  constraint pk_token primary key (token)
);

create table token_api (
  id                            bigserial not null,
  usuario_id                    bigint,
  codigo                        varchar(255),
  expiracao                     timestamp,
  constraint uq_token_api_usuario_id unique (usuario_id),
  constraint pk_token_api primary key (id)
);

create table trabalho (
  id                            bigserial not null,
  titulo                        varchar(350) not null,
  resumo                        varchar(600) not null,
  nome_arquivo                  varchar(400),
  data_cadastro                 date not null,
  data_alteracao                date,
  numero_acesso                 integer not null,
  idioma_id                     bigint,
  autores                       varchar(500) not null,
  palavra_chave                 varchar(200) not null,
  url                           varchar(500),
  constraint pk_trabalho primary key (id)
);

create table usuario (
  id                            bigserial not null,
  confirmacao_token             varchar(255),
  validado                      boolean,
  nome                          varchar(60) not null,
  email                         varchar(50) not null,
  senha                         varchar(255) not null,
  papel                         varchar(13),
  status                        boolean not null,
  data_cadastro                 date,
  data_alteracao                date,
  ultimo_acesso                 timestamp not null,
  constraint ck_usuario_papel check (papel in ('USUARIO','GERENTE','ADMINISTRADOR')),
  constraint uq_usuario_email unique (email),
  constraint pk_usuario primary key (id)
);

alter table artigo add constraint fk_artigo_idioma_id foreign key (idioma_id) references idioma (id) on delete restrict on update restrict;
create index ix_artigo_idioma_id on artigo (idioma_id);

alter table foto add constraint fk_foto_album_id foreign key (album_id) references album (id) on delete restrict on update restrict;
create index ix_foto_album_id on foto (album_id);

alter table marco add constraint fk_marco_categoria_id foreign key (categoria_id) references categoria (id) on delete restrict on update restrict;
create index ix_marco_categoria_id on marco (categoria_id);

alter table nota_tecnica add constraint fk_nota_tecnica_idioma_id foreign key (idioma_id) references idioma (id) on delete restrict on update restrict;
create index ix_nota_tecnica_idioma_id on nota_tecnica (idioma_id);

alter table publicacao add constraint fk_publicacao_idioma_id foreign key (idioma_id) references idioma (id) on delete restrict on update restrict;
create index ix_publicacao_idioma_id on publicacao (idioma_id);

alter table token_api add constraint fk_token_api_usuario_id foreign key (usuario_id) references usuario (id) on delete restrict on update restrict;

alter table trabalho add constraint fk_trabalho_idioma_id foreign key (idioma_id) references idioma (id) on delete restrict on update restrict;
create index ix_trabalho_idioma_id on trabalho (idioma_id);


# --- !Downs

alter table artigo drop constraint if exists fk_artigo_idioma_id;
drop index if exists ix_artigo_idioma_id;

alter table foto drop constraint if exists fk_foto_album_id;
drop index if exists ix_foto_album_id;

alter table marco drop constraint if exists fk_marco_categoria_id;
drop index if exists ix_marco_categoria_id;

alter table nota_tecnica drop constraint if exists fk_nota_tecnica_idioma_id;
drop index if exists ix_nota_tecnica_idioma_id;

alter table publicacao drop constraint if exists fk_publicacao_idioma_id;
drop index if exists ix_publicacao_idioma_id;

alter table token_api drop constraint if exists fk_token_api_usuario_id;

alter table trabalho drop constraint if exists fk_trabalho_idioma_id;
drop index if exists ix_trabalho_idioma_id;

drop table if exists album cascade;

drop table if exists artigo cascade;

drop table if exists avaliacao cascade;

drop table if exists categoria cascade;

drop table if exists contato cascade;

drop table if exists foto cascade;

drop table if exists home cascade;

drop table if exists idioma cascade;

drop table if exists log cascade;

drop table if exists marco cascade;

drop table if exists nota_tecnica cascade;

drop table if exists pais cascade;

drop table if exists publicacao cascade;

drop table if exists token cascade;

drop table if exists token_api cascade;

drop table if exists trabalho cascade;

drop table if exists usuario cascade;

