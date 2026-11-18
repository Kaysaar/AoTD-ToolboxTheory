package data.kaysaar.aotd.tot.recipes.impl;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.LinkedHashMap;
import java.util.Map;

public class CatalyticRefiningRecipe extends AoTDIndustryRecipePlugin {

    private static final String BONUS_ID = "aotd_catalytic_refining_bonus";

    @Override
    public Map<String, Integer> getVanillaProdUnits(Industry industry, int marketSize) {
        Map<String, Integer> units = new LinkedHashMap<>();
        units.put(Commodities.METALS, marketSize + 1);
        units.put(Commodities.RARE_METALS, Math.max(1, marketSize - 2));
        return units;
    }

    @Override
    public Map<String, Integer> getVanillaDemandUnits(Industry industry, int marketSize) {
        Map<String, Integer> units = new LinkedHashMap<>();
        units.put(Commodities.ORE, marketSize + 1);
        units.put(Commodities.RARE_ORE, Math.max(1, marketSize - 1));
        units.put(Commodities.VOLATILES, Math.max(1, marketSize - 3));
        return units;
    }

    @Override
    public void applyRecipeToIndustry(BaseIndustry industry) {
        int size = industry.getMarket().getSize();
        industry.demand(Commodities.ORE, size + 1);
        industry.demand(Commodities.RARE_ORE, Math.max(1, size - 1));
        industry.demand(Commodities.VOLATILES, Math.max(1, size - 3));
        industry.supply(Commodities.METALS, size + 1);
        industry.supply(Commodities.RARE_METALS, Math.max(1, size - 2));

        applyDeficitToProduction(industry,
                new String[]{Commodities.ORE, Commodities.VOLATILES},
                Commodities.METALS);
        applyDeficitToProduction(industry,
                new String[]{Commodities.RARE_ORE, Commodities.VOLATILES},
                Commodities.RARE_METALS);

        if (industry.getMaxDeficit(Commodities.VOLATILES).two == 0) {
            industry.getSupply(Commodities.METALS).getQuantity()
                    .modifyMult(BONUS_ID, 1.2f, "Catalytic process efficiency");
            industry.getSupply(Commodities.RARE_METALS).getQuantity()
                    .modifyMult(BONUS_ID, 1.2f, "Catalytic process efficiency");
        } else {
            unapply(industry);
        }
    }

    @Override
    public void unapply(BaseIndustry industry) {
        industry.getSupply(Commodities.METALS).getQuantity().unmodifyMult(BONUS_ID);
        industry.getSupply(Commodities.RARE_METALS).getQuantity().unmodifyMult(BONUS_ID);
    }

    @Override
    public void onRecipeRemoved(BaseIndustry industry) {
        industry.demand(Commodities.ORE, 0);
        industry.getDemand(Commodities.ORE).getQuantity().unmodifyFlat("ind_dr");
        industry.demand(Commodities.RARE_ORE, 0);
        industry.getDemand(Commodities.RARE_ORE).getQuantity().unmodifyFlat("ind_dr");
        industry.demand(Commodities.VOLATILES, 0);
        industry.getDemand(Commodities.VOLATILES).getQuantity().unmodifyFlat("ind_dr");
        industry.supply(Commodities.METALS, 0);
        industry.supply(2, Commodities.METALS, 0, "");
        industry.getSupply(Commodities.METALS).getQuantity().unmodifyFlat("ind_sb");
        industry.supply(Commodities.RARE_METALS, 0);
        industry.supply(2, Commodities.RARE_METALS, 0, "");
        industry.getSupply(Commodities.RARE_METALS).getQuantity().unmodifyFlat("ind_sb");
    }

    @Override
    public String getDescription(Industry industry) {
        return "Uses volatiles to refine ore and rare ore.";
    }

    @Override
    public void createAdditionalEffectsSection(TooltipMakerAPI tooltip) {
        tooltip.addSectionHeading("Additional effects", Alignment.MID, 12f);
        tooltip.addPara("Metals and rare metals output increase by 20% when volatiles demand is fully met.", 6f);
    }
}
