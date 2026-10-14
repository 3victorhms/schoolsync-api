package br.cefetmg.schoolsync_api.entity;

public enum TipoPeriodo {

    BIMESTRE(4, "Bimestre"),
    TRIMESTRE(3, "Trimestre"),
    SEMESTRE(2, "Semestre");

    private final int quantidade;
    private final String rotulo;

    TipoPeriodo(int quantidade, String rotulo) {
        this.quantidade = quantidade;
        this.rotulo = rotulo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String nomeDoPeriodo(int ordem) {
        return ordem + "º " + rotulo;
    }
}
