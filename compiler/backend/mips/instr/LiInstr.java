package backend.mips.instr;

import backend.register.Register;

public class LiInstr extends MipsInstr{
    private final Register rd;
    private final int imm;

    public LiInstr(Register rd, int imm) {
        this.rd = rd;
        this.imm = imm;
    }

    public String toString() {
        return "    li " + rd+", " + imm;
    }
}
