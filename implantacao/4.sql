create table nota_tecnica (
  id                            bigserial not null,
  titulo                        varchar(250) not null,
  resumo                        varchar(500) not null,
  url                           varchar(400),
  idioma_id                     bigint,
  autor                         varchar(500) not null,
  ano                           varchar(15) not null,
  data_cadastro                 date not null,
  data_alteracao                date,
  nome_capa                     varchar(250) not null,
  numero_acesso                 integer,
  palavra_chave                 varchar(200) not null,
  nome_arquivo                  varchar(300) not null,
  constraint pk_nota_tecnica primary key (id)
);

alter table nota_tecnica add constraint fk_nota_tecnica_idioma_id foreign key (idioma_id) references idioma (id) on delete restrict on update restrict;
create index ix_nota_tecnica_idioma_id on nota_tecnica (idioma_id);

alter table nota_tecnica alter column titulo type varchar(350);
alter table nota_tecnica alter column resumo type varchar(600);
alter table nota_tecnica alter column nome_arquivo type varchar(400);
alter table nota_tecnica alter column nome_capa type varchar(400);

alter table publicacao alter column titulo type varchar(350);
alter table publicacao alter column resumo type varchar(600);
alter table publicacao alter column nome_arquivo type varchar(400);
alter table publicacao alter column nome_capa type varchar(400);
alter table publicacao add column numero_acesso integer not null DEFAULT 0;

alter table trabalho alter column titulo type varchar(350);
alter table trabalho alter column resumo type varchar(600);
alter table trabalho alter column nome_arquivo type varchar(400);

alter table artigo alter column titulo type varchar(350);
alter table artigo alter column resumo type varchar(600);
alter table artigo alter column nome_arquivo type varchar(400);