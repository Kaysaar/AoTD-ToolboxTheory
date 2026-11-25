package data.kaysaar.aotd.tot.abilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class DeployFrontierKit extends BaseDurationAbility {
    public static final String ID = "aotd_deploy_frontier_kit";
    public static final String KIT = "aotd_frontier_development_kit";

    public static boolean canDeploy(CampaignFleetAPI fleet) {
        return getLocationRestriction(fleet) == null && hasKit(fleet);
    }

    private static boolean hasKit(CampaignFleetAPI fleet) {
        return fleet.getCargo().getCommodityQuantity(KIT) >= 1f;
    }

    private static String getLocationRestriction(CampaignFleetAPI fleet) {
        if (fleet == null || !fleet.isPlayerFleet() || fleet.isInHyperspace() || fleet.getStarSystem() == null) {
            return "Enter a star system to deploy the kit.";
        }
        for (MarketAPI market : Misc.getMarketsInLocation(fleet.getStarSystem())) {
            if (!market.isPlanetConditionMarketOnly()) {
                return "Deployment in colonized system blocked by frontier kit deployment parameters. It's protocols prohibits the kit from being activated in inhabited systems due to the extreme levels of the hyperwave pulses it can produce, far beyond safe levels that a non shielded human can withstand without severe DNA degradation.";
            }
        }
        return null;
    }

    @Override
    public boolean isUsable() {
        return super.isUsable() && canDeploy(getFleet()) && !Global.getSector().getCampaignUI().isShowingDialog();
    }

    @Override
    protected void activateImpl() {
        Global.getSector().getCampaignUI().showInteractionDialog(
                new RuleBasedInteractionDialogPluginImpl("AoTDFrontierKitMenu"), getFleet());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        if (!Global.CODEX_TOOLTIP_MODE) tooltip.addTitle(spec.getName());
        tooltip.addPara("Deploy a makeshift comm relay, sensor array, or nav buoy in a star system without colonies.", 5f);
        tooltip.addPara("Requires %s in fleet cargo. The device operates for %s.", 5f, Misc.getHighlightColor(),
                "1 Frontier Development Kit", "210 days");
        if (!Global.CODEX_TOOLTIP_MODE && getFleet() != null) {
            String restriction = getLocationRestriction(getFleet());
            if (restriction != null) tooltip.addPara(restriction, Misc.getNegativeHighlightColor(), 8f);
            if (!hasKit(getFleet())) tooltip.addPara("No Frontier Development Kit in fleet cargo.", Misc.getNegativeHighlightColor(), 5f);
        }
    }

    @Override
    protected String getActivationText() {
        return null;
    }

    @Override
    protected void applyEffect(float amount, float level) {
    }

    @Override
    protected void deactivateImpl() {
    }

    @Override
    protected void cleanupImpl() {
    }
}
