package backend.mips.instr;

import backend.register.Register;

public class MoveInstr extends MipsInstr{
    private final Register rd;
    private final Register rs;

    public MoveInstr(Register rd, Register rs) {
        this.rd = rd;
        this.rs = rs;
    }

    public String toString() {
        return "    move " + rd +", " + rs;
    }
}
