package data.kaysaar.aotd.tot.listeners;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityTooltipModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.abilities.DeployFrontierKit;

public class AoTDCommodityTooltipInjector implements CommodityTooltipModifier {
    @Override
    public void addSectionAfterPrice(TooltipMakerAPI info, float width, boolean expanded, CargoStackAPI stack) {
        if (!stack.isCommodityStack()) return;
        if (DeployFrontierKit.KIT.equals(stack.getCommodityId())) {
            info.addSectionHeading("Orbital deployment", Alignment.MID, 10f);
            info.addPara("Each kit contains a modular orbital platform. In an uncolonized system, use %s to configure it as a %s, %s, or %s.",
                    5f, Misc.getHighlightColor(), "Deploy Frontier Kit ability", "makeshift comm relay", "makeshift sensor array", "makeshift nav buoy");
            info.addPara("Once deployed, the platform operates in orbit for %s.", 5f, Misc.getHighlightColor(), "210 days");
        }
        if (expanded) {
            info.addCustom(new AoTDPriceTableRemoval(info,stack.getCommodityId()).getMainPanel(),0f);
        }
    }
}
