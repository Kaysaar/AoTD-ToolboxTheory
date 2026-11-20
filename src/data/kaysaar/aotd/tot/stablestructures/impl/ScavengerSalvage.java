package data.kaysaar.aotd.tot.stablestructures.impl;

import com.fs.starfarer.api.impl.campaign.ids.Commodities;

import java.util.SplittableRandom;

public enum ScavengerSalvage {
    METALS(Commodities.METALS, 1, 100, 400, 1000),
    MACHINERY(Commodities.HEAVY_MACHINERY, 1, 100, 50, 150),
    HULLS(Commodities.SHIPS, 1, 100, 200, 800),
    TRANSPLUTONICS(Commodities.RARE_METALS, 2, 25, 100, 120),
    GOODS(Commodities.DOMESTIC_GOODS, 2, 40, 100, 150),
    DRUGS(Commodities.DRUGS, 4, 2, 250, 350),
    WEAPONS(Commodities.HAND_WEAPONS, 4, 2, 150, 500),
    ORGANS(Commodities.ORGANS, 4, 1, 200, 400);

    final String commodity;
    final int fieldsRequired;
    final int chance;
    final int min;
    final int max;

    ScavengerSalvage(String commodity, int fieldsRequired, int chance, int min, int max) {
        this.commodity = commodity;
        this.fieldsRequired = fieldsRequired;
        this.chance = chance;
        this.min = min;
        this.max = max;
    }

    public int getMonthlyAmount(int fields, long seed) {
        if (fields < fieldsRequired) return 0;
        SplittableRandom random = new SplittableRandom(seed + commodity.hashCode());
        int amount = 0;
        for (int i = 0; i < fields; i++) {
            if (random.nextInt(100) < chance) amount += random.nextInt(min, max + 1);
        }
        return amount;
    }
}