package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.ImagePanel;
import ashlib.data.plugins.ui.plugins.UILinesRenderer;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLine;

public class ProductionPlanShowcase {
    public static final float BODY_WIDTH = 400f;
    private static final AoTDIndustryRecipePlugin DEFAULT = new AoTDIndustryRecipePlugin();

    public static void addTo(CustomPanelAPI parent, TooltipMakerAPI tooltip, AoTDProductionLine plan,
                             boolean current) {
        tooltip.setParaFont(Fonts.ORBITRON_20AABOLD);
        tooltip.addPara(plan.spec.getName(), Misc.getTooltipTitleAndLightHighlightColor(), 0f)
                .setAlignment(Alignment.MID);
        tooltip.setParaFontDefault();

        ImagePanel image = new ImagePanel();
        UILinesRenderer border = new UILinesRenderer(0f);
        CustomPanelAPI row = parent.createCustomPanel(BODY_WIDTH, 95f, border);
        CustomPanelAPI picture = row.createCustomPanel(190f, 95f, image);
        image.init(picture, Global.getSettings().getSprite(plan.spec.getImageName()));
        border.setPanel(picture);
        row.addComponent(picture).inTL((BODY_WIDTH - 190f) / 2f, 0f);
        tooltip.addCustom(row, 10f);
        tooltip.addSectionHeading(current ? "Current Production Plan" : "Selected Production Plan", Alignment.MID, 12f);
        tooltip.addPara(plan.getRecipeName(), plan.getRecipeColor(), 6f).setAlignment(Alignment.MID);
        tooltip.addSectionHeading("", Alignment.MID, 6f);
        if (plan.error != null || plan.industry == null) {
            tooltip.addPara(plan.error == null ? "Industry preview unavailable." : plan.error,
                    Misc.getNegativeHighlightColor(), 10f);
            return;
        }
        AoTDIndustryRecipePlugin recipe = plan.recipe == null ? DEFAULT : plan.recipe;
        recipe.createIndustryTooltipBody(plan.industry, Industry.IndustryTooltipMode.NORMAL,
                tooltip, true, BODY_WIDTH, true);
    }
}
