create table oportunidade_rd (
  id                            bigserial not null,
  codigo                        varchar(100) not null,
  nome                          varchar(200) not null,
  id_campo_custom               varchar(200) not null,
  valor_campo_custom            varchar(200) not null,
  data_cadastro                 timestamp not null,
  constraint pk_oportunidade_rd primary key (id)
);
alter table proposta_rd alter column codigo_proposta type varchar(100);

alter table proposta_rd add column id_campo_custom varchar(200);
alter table proposta_rd add column valor_campo_custom varchar(200);
alter table proposta_rd add column numero integer;

delete from proposta_rd;
drop table proposta_rd;

insert into proposta_rd (numero, codigo_proposta, nome_proposta, id_campo_custom, valor_campo_custom)
                 values (1, '5ddd14482bf5a9001525e7af', 'Oportunidade 5', '5dd2ea54c6ab840010cff46d', '001-2019');


