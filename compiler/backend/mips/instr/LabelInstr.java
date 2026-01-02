package backend.mips.instr;

public class LabelInstr extends MipsInstr{
    private final String label;

    public LabelInstr(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label+":";
    }
}
