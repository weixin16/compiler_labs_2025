package backend.mips.instr;

import backend.register.Register;

public class BeqzInstr extends MipsInstr {
    private final Register rs;    //addu, subu, mul
    private final String label;

    public BeqzInstr(Register rs, String label) {
        this.rs = rs;
        this.label = label;
    }

    @Override
    public String toString() {
        return "    beq "+rs+", $zero, "+label;
    }
}
