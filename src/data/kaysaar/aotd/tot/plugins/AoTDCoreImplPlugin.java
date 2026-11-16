package data.kaysaar.aotd.tot.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.CoreCampaignPluginImpl;
import com.fs.starfarer.api.impl.campaign.PlanetInteractionDialogPluginImpl;

public class AoTDCoreImplPlugin extends CoreCampaignPluginImpl {
    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        if (interactionTarget instanceof PlanetAPI) {
            return new PluginPick<InteractionDialogPlugin>(new AoTDPlanetInteractionDialogPluginImpl(), PickPriority.MOD_GENERAL);
        }
        return super.pickInteractionDialogPlugin(interactionTarget);
    }

    @Override
    public String getId() {
        return "AoTDCoreImplPlugin";
    }
}
