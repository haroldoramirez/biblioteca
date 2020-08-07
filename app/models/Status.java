package models;

public enum Status {
    APROVADO(1), REPROVADO(2), AVALIAR(3);

    public int valorStatus;

    Status(int valor) {
        valorStatus = valor;
    }
}
