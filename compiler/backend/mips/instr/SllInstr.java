package backend.mips.instr;

import backend.register.Register;

public class SllInstr extends MipsInstr{
    private final Register rd;
    private final Register rs;
    private final int shamt;

    public SllInstr(Register rd, Register rs, int shamt) {
        this.rd = rd;
        this.rs = rs;
        this.shamt = shamt;
    }

    @Override
    public String toString() {
        return "    sll " + rd + ", " + rs +", " + shamt;
    }
}
