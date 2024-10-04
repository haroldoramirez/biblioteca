# --- Sample dataset

# --- !Ups

insert into pais (id, nome, data_cadastro, data_alteracao) values (1, 'Argentina', '2024-10-04', '2024-10-04');
insert into pais (id, nome, data_cadastro, data_alteracao) values (2,'Brazil', '2024-10-04', '2024-10-04');
insert into pais (id, nome, data_cadastro, data_alteracao) values (3, 'Paraguay', '2024-10-04', '2024-10-04');

insert into usuario (id, nome, email, senha, papel, status, validado, data_cadastro, data_alteracao, ultimo_acesso)
values (1, 'Haroldo Ramirez da Nóbrega', 'haroldo.nobrega@cibiogas.org', '$2a$10$nbLHwCCpkqWbkUZEDX3G5e3CRDJuqTPgVRRfaNduKRiBdN8jVu7ai', 'ADMINISTRADOR', TRUE, TRUE, '2024-10-04', '2024-10-04', '2024-10-04');
insert into usuario (id, nome, email, senha, papel, status, validado, data_cadastro, data_alteracao, ultimo_acesso)
values (2, 'Administrador', 'administrador@cibiogas.org', '$2a$10$nbLHwCCpkqWbkUZEDX3G5e3CRDJuqTPgVRRfaNduKRiBdN8jVu7ai', 'ADMINISTRADOR', TRUE, TRUE, '2024-10-04', '2024-10-04', '2024-10-04');
insert into usuario (id, nome, email, senha, papel, status, validado, data_cadastro, data_alteracao, ultimo_acesso)
values (3, 'Gerente', 'gerente@cibiogas.org', '$2a$10$nbLHwCCpkqWbkUZEDX3G5e3CRDJuqTPgVRRfaNduKRiBdN8jVu7ai', 'GERENTE', TRUE, TRUE, '2024-10-04', '2024-10-04', '2024-10-04');
insert into usuario (id, nome, email, senha, papel, status, validado, data_cadastro, data_alteracao, ultimo_acesso)
values (4, 'Usuário', 'usuario@cibiogas.org', '$2a$10$nbLHwCCpkqWbkUZEDX3G5e3CRDJuqTPgVRRfaNduKRiBdN8jVu7ai', 'USUARIO', TRUE, TRUE, '2024-10-04', '2024-10-04', '2024-10-04');

insert into categoria (id, nome, data_cadastro, data_alteracao)
values (1, 'Planos e Leis', '2024-10-04', '2024-10-04');
insert into categoria (id, nome, data_cadastro, data_alteracao)
values (2, 'Regulação', '2024-10-04', '2024-10-04');

insert into evento (id, nome, data_inicio, data_fim, site, localidade, instituicao, data_cadastro, data_alteracao)
values (1,'Seminário Técnico sobre Geração Distribuída',  '2024-10-04', '2024-10-04', 'https://www.abiogas.org.br/seminariotecnicogdbi', 'São Paulo SP', 'Associação Brasileira de Biogás e Biometano (Abiogás)', '2024-10-04', '2024-10-04');

insert into site (id, titulo, url, pais_id, data_cadastro, data_alteracao)
values (1, 'Abiogás', 'https://www.abiogas.org.br/', 2, '2024-10-04', '2024-10-04');
insert into site (id, titulo, url, pais_id, data_cadastro, data_alteracao)
values (2, 'Embrapa', 'https://www.embrapa.br/', 2, '2024-10-04', '2024-10-04');

insert into idioma (id, nome, data_cadastro, data_alteracao) values (1, 'Português', '2024-10-04', '2024-10-04');
insert into idioma (id, nome, data_cadastro, data_alteracao) values (2, 'Ingles', '2024-10-04', '2024-10-04');
insert into idioma (id, nome, data_cadastro, data_alteracao) values (3, 'Espanhol', '2024-10-04', '2024-10-04');