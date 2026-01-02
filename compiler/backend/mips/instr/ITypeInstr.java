package backend.mips.instr;

import backend.register.Register;

public class ITypeInstr extends MipsInstr{
    private final String op;    //addiu
    private final Register rd;
    private final Register rs;
    private final int imm;

    public ITypeInstr(String op, Register rd, Register rs, int imm) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.imm = imm;
    }

    @Override
    public String toString() {
        return "    " + op +" " + rd + ", " + rs +", " + imm;
    }
}
