package backend.mips.instr;

import backend.register.Register;

public class RTypeInstr extends MipsInstr{
    private final String op;    //addu, subu, mul
    private final Register rd;
    private final Register rs;
    private final Register rt;

    public RTypeInstr(String op, Register rd, Register rs, Register rt) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }

    @Override
    public String toString() {
        return "    " + op +" " + rd + ", " + rs +", " + rt;
    }
}
