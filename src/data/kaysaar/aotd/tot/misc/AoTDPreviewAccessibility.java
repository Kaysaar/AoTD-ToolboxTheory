package data.kaysaar.aotd.tot.misc;

import com.fs.starfarer.api.combat.StatBonus;

public class AoTDPreviewAccessibility extends StatBonus {
    @Override
    public float computeEffective(float base) {
        return 1f;
    }
}
