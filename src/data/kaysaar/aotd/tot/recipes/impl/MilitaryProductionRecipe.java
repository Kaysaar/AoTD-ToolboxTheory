package data.kaysaar.aotd.tot.recipes.impl;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.LinkedHashMap;
import java.util.Map;

public class MilitaryProductionRecipe extends AoTDIndustryRecipePlugin {

    private static final String BONUS_ID = "aotd_military_production_bonus";

    @Override
    public Map<String, Integer> getVanillaProdUnits(Industry industry, int marketSize) {
        Map<String, Integer> units = new LinkedHashMap<>();
        units.put(Commodities.SUPPLIES, Math.max(1, marketSize - 2));
        units.put(Commodities.HAND_WEAPONS, Math.max(1, marketSize - 1));
        return units;
    }

    @Override
    public Map<String, Integer> getVanillaDemandUnits(Industry industry, int marketSize) {
        Map<String, Integer> units = new LinkedHashMap<>();
        units.put(Commodities.METALS, marketSize + 1);
        units.put(Commodities.RARE_METALS, Math.max(1, marketSize - 2));
        units.put(Commodities.HEAVY_MACHINERY, Math.max(1, marketSize - 3));
        return units;
    }

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        int size = industry.getMarket().getSize();
        industry.demand(Commodities.METALS, size + 1);
        industry.demand(Commodities.RARE_METALS, Math.max(1, size - 2));
        industry.demand(Commodities.HEAVY_MACHINERY, Math.max(1, size - 3));
        industry.supply(Commodities.SUPPLIES, Math.max(1, size - 2));
        industry.supply(Commodities.HAND_WEAPONS, Math.max(1, size - 1));

        applyDeficitToProduction(industry,
                new String[]{Commodities.METALS, Commodities.HEAVY_MACHINERY},
                Commodities.SUPPLIES);
        applyDeficitToProduction(industry,
                new String[]{Commodities.METALS, Commodities.RARE_METALS},
                Commodities.HAND_WEAPONS);

        if (industry.getMaxDeficit(Commodities.RARE_METALS).two == 0) {
            industry.getSupply(Commodities.HAND_WEAPONS).getQuantity()
                    .modifyMult(BONUS_ID, 1.25f, "Specialized weapons manufacturing");
        } else {
            unapply(industry);
        }
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.getSupply(Commodities.HAND_WEAPONS).getQuantity().unmodifyMult(BONUS_ID);
    }

    @Override
    public void onRecipeRemoved(BaseIndustry industry) {
        industry.demand(Commodities.METALS, 0);
        industry.getDemand(Commodities.METALS).getQuantity().unmodifyFlat("ind_dr");
        industry.demand(Commodities.RARE_METALS, 0);
        industry.getDemand(Commodities.RARE_METALS).getQuantity().unmodifyFlat("ind_dr");
        industry.demand(Commodities.HEAVY_MACHINERY, 0);
        industry.getDemand(Commodities.HEAVY_MACHINERY).getQuantity().unmodifyFlat("ind_dr");
        industry.supply(Commodities.SUPPLIES, 0);
        industry.supply(2, Commodities.SUPPLIES, 0, "");
        industry.getSupply(Commodities.SUPPLIES).getQuantity().unmodifyFlat("ind_sb");
        industry.supply(Commodities.HAND_WEAPONS, 0);
        industry.supply(2, Commodities.HAND_WEAPONS, 0, "");
        industry.getSupply(Commodities.HAND_WEAPONS).getQuantity().unmodifyFlat("ind_sb");
    }

    @Override
    public String getDescription(Industry industry) {
        return "Prioritizes supplies and armaments.";
    }

    @Override
    public void createAdditionalEffectsSection(TooltipMakerAPI tooltip) {
        tooltip.addSectionHeading("Additional effects", Alignment.MID, 12f);
        tooltip.addPara("%s output increases by %s when %s demand is fully met.", 6f,
                com.fs.starfarer.api.util.Misc.getHighlightColor(), "Hand weapons", "25%", "Rare Metals");
    }
}
