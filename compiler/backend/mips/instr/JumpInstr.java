package backend.mips.instr;

public class JumpInstr extends MipsInstr{
    private final String op;    //j, jal
    private final String target;

    public JumpInstr(String op, String target) {
        this.op = op;
        this.target = target;
    }

    public String toString() {
        return "    " + op +" " + target;
    }
}
