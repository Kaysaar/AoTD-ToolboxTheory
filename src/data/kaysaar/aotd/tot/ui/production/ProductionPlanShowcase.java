package data.kaysaar.aotd.tot.ui.production;

import java.util.ArrayList;
import ashlib.data.plugins.ui.models.ButtonWithImageComponent;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.misc.AoTDPreviewMarket;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;

public class ProductionPlanShowcase {
    public static final float BODY_WIDTH = 400f;
    private static final AoTDIndustryRecipePlugin DEFAULT = new AoTDIndustryRecipePlugin();

    public static void addTo(CustomPanelAPI parent, TooltipMakerAPI tooltip, AoTDProductionLine plan,
                             boolean current) {
        tooltip.setParaFont(Fonts.ORBITRON_20AABOLD);
        tooltip.addPara(plan.getIndustryName(), Misc.getTooltipTitleAndLightHighlightColor(), 2f)
                .setAlignment(Alignment.MID);
        tooltip.setParaFontDefault();

        ButtonWithImageComponent picture = new ButtonWithImageComponent(190f, 95f, plan.getIndustryImage());
        picture.setImageFrameAppearance(Misc.getDarkPlayerColor());
        CustomPanelAPI row = parent.createCustomPanel(BODY_WIDTH, 95f, null);
        row.addComponent(picture.getComponentPanel()).inTL((BODY_WIDTH - 190f) / 2f, 0f);
        tooltip.addCustom(row, 10f);
        tooltip.addSectionHeading(current ? "Current Production Plan" : "Selected Production Plan", Alignment.MID, 12f);
        tooltip.addPara(plan.getRecipeName(), plan.getRecipeColor(), 6f).setAlignment(Alignment.MID);
        tooltip.addSectionHeading("", Alignment.MID, 6f);
        if (plan.error != null || plan.industry == null) {
            tooltip.addPara(plan.error == null ? "Industry preview unavailable." : plan.error,
                    Misc.getNegativeHighlightColor(), 10f);
            return;
        }
        if (plan.pending) {
            tooltip.addPara("Expected supply and demand once this industry starts operating.", Misc.getHighlightColor(), 6f);
        }
        if (plan.local) {
            tooltip.addPara("Output assumes %s. Local shortages can reduce production.", 6f,
                    Misc.getHighlightColor(), "all input demand is met");
        }
        AoTDIndustryRecipePlugin recipe = plan.recipe == null ? DEFAULT : plan.recipe;
        AoTDPreviewMarket.withFullAvailability(plan.industry.getMarket(), () ->
                recipe.createIndustryTooltipBody(plan.industry, Industry.IndustryTooltipMode.NORMAL,
                        tooltip, true, BODY_WIDTH, true, new ArrayList<>(plan.production.keySet()), new ArrayList<>(plan.demand.keySet())));
    }
}
