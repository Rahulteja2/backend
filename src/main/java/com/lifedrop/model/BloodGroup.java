package com.lifedrop.model;

import java.util.Arrays;
import java.util.List;

public enum BloodGroup {
    O_NEG("O", false), O_POS("O", true),
    A_NEG("A", false), A_POS("A", true),
    B_NEG("B", false), B_POS("B", true),
    AB_NEG("AB", false), AB_POS("AB", true);

    private final String abo;
    private final boolean rhPositive;

    BloodGroup(String abo, boolean rhPositive) {
        this.abo = abo;
        this.rhPositive = rhPositive;
    }

    /** True if a donor of this group can give red cells to the recipient. */
    public boolean canGiveTo(BloodGroup recipient) {
        boolean aboOk = abo.equals("O") || recipient.abo.equals("AB") || abo.equals(recipient.abo);
        boolean rhOk = !rhPositive || recipient.rhPositive;
        return aboOk && rhOk;
    }

    /** All donor groups compatible with the given patient group. */
    public static List<BloodGroup> donorsFor(BloodGroup recipient) {
        return Arrays.stream(values()).filter(g -> g.canGiveTo(recipient)).toList();
    }
}