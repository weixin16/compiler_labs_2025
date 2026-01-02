package backend.mips.instr;

import backend.register.Register;

public class DivModInstr extends MipsInstr{
    private final String op;    //div,mfhi,mflo
    private final Register rd;
    private final Register rs;
    private final Register rt;

    public DivModInstr(String op, Register rs, Register rt) {
        this.op = op;
        this.rs = rs;
        this.rt = rt;
        this.rd = null;
    }

    public DivModInstr(String op, Register rd) {
        this.op = op;
        this.rs = null;
        this.rt = null;
        this.rd = rd;
    }

    @Override
    public String toString() {
        return switch (op){
            case "div" ->  "    div " + rs + ", "+rt;
            case "mfhi" -> "    mfhi "+rd;
            case "mflo" -> "    mflo "+rd;
            default -> " ";
        };
    }
}
