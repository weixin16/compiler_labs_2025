package backend.mips.instr;

import backend.register.Register;

public class JrInstr extends MipsInstr{
    private final Register rs;

    public JrInstr(Register rs) {
        this.rs = rs;
    }

    @Override
    public String toString() {
        return "    jr "+rs;
    }
}
