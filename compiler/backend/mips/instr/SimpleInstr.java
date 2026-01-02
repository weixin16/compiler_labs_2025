package backend.mips.instr;

public class SimpleInstr extends MipsInstr{
    private final String str;

    public SimpleInstr(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
