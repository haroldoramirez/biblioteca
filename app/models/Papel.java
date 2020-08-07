package models;

public enum Papel {
    USUARIO(1), GERENTE(2), ADMINISTRADOR(3);

    public int valorPapel;

    Papel(int valor) {
        valorPapel = valor;
    }
}
