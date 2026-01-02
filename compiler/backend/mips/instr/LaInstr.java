package backend.mips.instr;

import backend.register.Register;

public class LaInstr extends MipsInstr{
    private final Register rd;
    private final String label;

    public LaInstr(Register rd, String label) {
        this.rd = rd;
        this.label = label;
    }

    public String toString() {
        return "    la " + rd +", " + label;
    }
}
