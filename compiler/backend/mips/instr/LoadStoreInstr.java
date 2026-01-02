package backend.mips.instr;

import backend.register.Register;

public class LoadStoreInstr extends MipsInstr{
    private final String op;    //lw, sw
    private final Register rt;
    private final int offset;
    private final Register base;

    public LoadStoreInstr(String op, Register rt, int offset, Register base) {
        this.op = op;
        this.rt = rt;
        this.offset = offset;
        this.base = base;
    }

    @Override
    public String toString() {
        return "    " + op +" " + rt+ ", " + offset + "("+ base+ ")";
    }
}
