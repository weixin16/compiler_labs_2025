package backend.mips.instr;

public class BranchInstr extends MipsInstr{
    private final String op;
    private final String rs;
    private final String rt;
    private final String label;

    public BranchInstr(String op, String rs, String rt, String label) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.label = label;
    }

    @Override
    public String toString() {
        return "    " + op +" " + rs + ", " + rt +", " + label;
    }

}
