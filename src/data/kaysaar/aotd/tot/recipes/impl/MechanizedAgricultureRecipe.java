package data.kaysaar.aotd.tot.recipes.impl;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.LinkedHashMap;
import java.util.Map;

public class MechanizedAgricultureRecipe extends AoTDIndustryRecipePlugin {

    private static final String BONUS_ID = "aotd_mechanized_agriculture_bonus";

    @Override
    public Map<String, Integer> getVanillaProdUnits(Industry industry, int marketSize) {
        Map<String, Integer> units = new LinkedHashMap<>();
        units.put(Commodities.FOOD, marketSize + 1);
        return units;
    }

    @Override
    public Map<String, Integer> getVanillaDemandUnits(Industry industry, int marketSize) {
        Map<String, Integer> units = new LinkedHashMap<>();
        units.put(Commodities.HEAVY_MACHINERY, Math.max(1, marketSize - 2));
        units.put(Commodities.ORGANICS, Math.max(1, marketSize - 3));
        return units;
    }

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        int size = industry.getMarket().getSize();
        industry.demand(Commodities.HEAVY_MACHINERY, Math.max(1, size - 2));
        industry.demand(Commodities.ORGANICS, Math.max(1, size - 3));
        industry.supply(Commodities.FOOD, size + 1);

        applyDeficitToProduction(industry,
                new String[]{Commodities.HEAVY_MACHINERY, Commodities.ORGANICS},
                Commodities.FOOD);

        if (industry.getMaxDeficit(Commodities.HEAVY_MACHINERY).two == 0) {
            industry.getSupply(Commodities.FOOD).getQuantity()
                    .modifyMult(BONUS_ID, 1.25f, "Fully mechanized cultivation");
        } else {
            unapply(industry);
        }
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.getSupply(Commodities.FOOD).getQuantity().unmodifyMult(BONUS_ID);
    }

    @Override
    public void onRecipeRemoved(BaseIndustry industry) {
        industry.demand(Commodities.HEAVY_MACHINERY, 0);
        industry.getDemand(Commodities.HEAVY_MACHINERY).getQuantity().unmodifyFlat("ind_dr");
        industry.demand(Commodities.ORGANICS, 0);
        industry.getDemand(Commodities.ORGANICS).getQuantity().unmodifyFlat("ind_dr");
        industry.supply(Commodities.FOOD, 0);
        industry.supply(2, Commodities.FOOD, 0, "");
        industry.getSupply(Commodities.FOOD).getQuantity().unmodifyFlat("ind_sb");
    }

    @Override
    public String getDescription(Industry industry) {
        return "Uses heavy machinery and organics to grow food.";
    }

    @Override
    public void createAdditionalEffectsSection(TooltipMakerAPI tooltip) {
        tooltip.addSectionHeading("Additional effects", Alignment.MID, 12f);
        tooltip.addPara("Food output increases by 25% when heavy machinery demand is fully met.", 6f);
    }
}
