package data.kaysaar.aotd.tot.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.PlanetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import data.kaysaar.aotd.tot.misc.AoTDToolboxMisc;


public class AoTDPlanetInteractionDialogPluginImpl extends PlanetInteractionDialogPluginImpl {

    private InteractionDialogAPI dialog;
    private PlanetAPI planet;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        this.planet = (PlanetAPI) dialog.getInteractionTarget();
        super.init(dialog);
    }


    public static int getNumStableLocations(StarSystemAPI system) {
        if (system == null) return 0;
        return AoTDToolboxMisc.calculateNaturalSystemStructureCapacity(system).stableLocations;
    }

    public static int getAvailableStableLocations(StarSystemAPI system) {
        if (system == null) return 0;
        return AoTDToolboxMisc.calculateNaturalSystemStructureCapacity(system).availableStableLocations;
    }


    public static int getOccupiedStableLocations(StarSystemAPI system) {
        if (system == null) return 0;
        return AoTDToolboxMisc.calculateNaturalSystemStructureCapacity(system).occupiedStableLocations;
    }

    @Override
    public void optionSelected(String text, Object optionData) {
        if (optionData != OptionId.ADD_STABLE_DESCRIBE) {
            super.optionSelected(text, optionData);
            return;
        }

        if (text != null) {
            dialog.addOptionSelectedText(optionData);
        }
        String corona = planet.getSpec().isBlackHole()
                ? "near the event horizon"
                : "in the star's corona";
        dialog.getTextPanel().addParagraph(
                "The procedure requires spreading prodigious amounts of antimatter " + corona + ", "
                        + "according to calculations far beyond the ability of anything on the right side of the "
                        + "treaty that ended the Second AI War.");

        boolean canAfford = dialog.getTextPanel().addCostPanel(
                "Resources required (available)",
                Commodities.ALPHA_CORE, 1, false,
                Commodities.HEAVY_MACHINERY, STABLE_MACHINERY_REQ, false,
                Commodities.FUEL, STABLE_FUEL_REQ, true);

        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();

        int num = getNumStableLocations(planet.getStarSystem());
        boolean alreadyCant = false;

        if (num <= 0) {
            options.addOption("Proceed with the operation", OptionId.ADD_STABLE_CONFIRM, null);
        } else if (num < 2) {
            dialog.getTextPanel().addParagraph(
                    "Normally, this procedure can only be performed in a star system without any "
                            + "stable locations. However, your chief engineer suggests an unorthodox workaround.");
            options.addOption("Proceed with the operation", OptionId.ADD_STABLE_CONFIRM, null);
            SetStoryOption.set(
                    dialog,
                    Global.getSettings().getInt("createStableLocation"),
                    OptionId.ADD_STABLE_CONFIRM,
                    "createStableLocation",
                    Sounds.STORY_POINT_SPEND_TECHNOLOGY,
                    "Created additional stable location in "
                            + planet.getStarSystem().getNameWithLowercaseType());
        } else {
            alreadyCant = true;
            String reason = "This procedure can not performed in a star system that already has "
                    + "numerous stable locations.";
            options.addOption("Proceed with the operation", OptionId.ADD_STABLE_CONFIRM, null);
            options.setEnabled(OptionId.ADD_STABLE_CONFIRM, false);
            dialog.getTextPanel().addParagraph(reason);
            options.setTooltip(OptionId.ADD_STABLE_CONFIRM, reason);
        }

        if (!canAfford && !alreadyCant) {
            String reason = "You do not have the necessary resources to carry out this procedure.";
            options.setEnabled(OptionId.ADD_STABLE_CONFIRM, false);
            dialog.getTextPanel().addParagraph(reason);
            options.setTooltip(OptionId.ADD_STABLE_CONFIRM, reason);
        }

        options.addOption("Never mind", OptionId.ADD_STABLE_NEVER_MIND, null);
    }
}
