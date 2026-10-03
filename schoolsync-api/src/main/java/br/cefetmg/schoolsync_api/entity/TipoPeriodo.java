package br.cefetmg.schoolsync_api.entity;

/**
 * Como o ano letivo da sala é dividido. O líder escolhe ao criar a sala
 * e cada tipo define quantos períodos a sala precisa ter.
 */
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

    /** Ex.: nomeDoPeriodo(2) em uma sala por bimestre = "2º Bimestre". */
    public String nomeDoPeriodo(int ordem) {
        return ordem + "º " + rotulo;
    }
}
