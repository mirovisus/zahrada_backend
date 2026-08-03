package upce.fei.garden.model.enums;

/**
 * Naléhavost realizace poptávky - nahrazuje dřívější konkrétní požadované datum.
 */

public enum DemandUrgency {
    CO_NEJDRIVE("Co nejdříve"),
    DO_TYDNE("Do týdne"),
    DO_MESICE("Do měsíce"),
    DO_TRI_MESICU("Do tří měsíců"),
    FLEXIBILNI("Flexibilní - domluva se zhotovitelem");

    private final String label;

    DemandUrgency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
