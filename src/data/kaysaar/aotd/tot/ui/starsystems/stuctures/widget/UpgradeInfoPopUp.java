package data.kaysaar.aotd.tot.ui.starsystems.stuctures.widget;

import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.kaysaar.aotd.tot.misc.AoTDToolboxMisc;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureImpl;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpec;
import data.kaysaar.aotd.tot.stablestructures.AoTDStableStructureSpecManager;
import data.kaysaar.aotd.tot.ui.commoditypanel.AoTDCommodityShortPanelCombined;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpgradeInfoPopUp extends BasePopUpDialog {

    private static final int RESOURCE_COLUMNS = 5;
    private final StableStructureWidget widget;
    private final AoTDStableStructureImpl upgrade;

    public UpgradeInfoPopUp(StableStructureWidget widget, AoTDStableStructureSpec upgradeSpec) {
        super("Confirm Structure Upgrade");
        this.widget = widget;
        this.upgrade = upgradeSpec.getImplClassInstance();
        confirmButtonText = "Upgrade";
    }

    private boolean isValidUpgradeTarget() {
        AoTDStableStructureImpl current = widget.getStructure();
        if (current == null || !current.isCurrentImplementation() || !current.isPlayerOwned()
                || current.isBuilding() || current.isDisrupted()
                || current.getCurrentlyTiedEntity() == null
                || AoTDToolboxMisc.isStoryCriticalStructure(current.getCurrentlyTiedEntity())) return false;

        // The confirmation may stay open while the underlying structure changes.
        boolean directUpgrade = false;
        for (AoTDStableStructureSpec candidate : AoTDStableStructureSpecManager.getUpgradeChildren(current.id)) {
            if (candidate.getId().equals(upgrade.id)) {
                directUpgrade = true;
                break;
            }
        }
        return directUpgrade && upgrade.isAvailableToBuild(
                current.getCurrentlyTiedEntity().getStarSystem(),
                Global.getSector().getPlayerFaction());
    }

    private boolean hasEnoughCredits() {
        return Global.getSector().getPlayerFleet().getCargo().getCredits().get()
                >= Math.max(0f, upgrade.getBuildCostCredits());
    }

    private boolean canUpgrade() {
        return isValidUpgradeTarget() && hasEnoughCredits();
    }

    @Override
    public ButtonAPI generateConfirmButton(TooltipMakerAPI tooltip) {
        ButtonAPI button = super.generateConfirmButton(tooltip);
        button.setButtonPressedSound("ui_upgrade_industry");
        button.setShortcut(Keyboard.KEY_U, false);
        button.setEnabled(canUpgrade());
        return button;
    }

    private LinkedHashMap<String, Integer> getResourceCosts(boolean specialItems) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> costs = upgrade.getBuildCostResources();
        if (costs == null) return result;

        for (Map.Entry<String, Integer> entry : costs.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) continue;
            boolean commodity = Global.getSettings().getCommoditySpec(entry.getKey()) != null;
            boolean special = Global.getSettings().getSpecialItemSpec(entry.getKey()) != null;
            if (specialItems && !commodity && special) result.put(entry.getKey(), entry.getValue());
            if (!specialItems && commodity) result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public float getPreferredHeight() {
        int materials = getResourceCosts(false).size();
        int items = getResourceCosts(true).size();
        int rows = (materials + RESOURCE_COLUMNS - 1) / RESOURCE_COLUMNS;
        float height = 265f + rows * 43f + (items > 0 ? 92f + Math.max(0, (items - 1) / 4) * 37f : 0f);
        if (Global.getSector().getPlayerFleet().getCargo().getCredits().get() < upgrade.getBuildCostCredits()) {
            height += 40f;
        }
        return Math.min(height, Global.getSettings().getScreenHeight() - 20f);
    }

    @Override
    public void createContentForDialog(TooltipMakerAPI tooltip, float width) {
        tooltip.setParaFont(Fonts.INSIGNIA_LARGE);
        AoTDStableStructureImpl current = widget.getStructure();
        if (!isValidUpgradeTarget()) {
            tooltip.addPara("The selected structure is no longer available as an upgrade in this star system.",
                    Misc.getNegativeHighlightColor(), 3f);
            return;
        }

        int days = Math.max(0, (int) Math.ceil(upgrade.getBuildTime()));
        String duration = days + (days == 1 ? " day" : " days");
        String cost = Misc.getDGSCredits(upgrade.getBuildCostCredits());

        tooltip.addPara(
                "Upgrading %s to %s will take %s and cost %s credits. The following resources are required:",
                2f, Color.ORANGE, current.getName(), upgrade.getName(), duration, cost);

        tooltip.addSectionHeading("Resource Cost", Alignment.MID, 10f);
        LinkedHashMap<String, Integer> materials = getResourceCosts(false);
        LinkedHashMap<String, Integer> items = getResourceCosts(true);

        if (!materials.isEmpty()) {
            tooltip.addCustom(new AoTDCommodityShortPanelCombined(width, RESOURCE_COLUMNS, materials).getMainPanel(), 5f);
            int months = Math.max(1, (int) Math.ceil(days / 30f));
            tooltip.addPara(
                    "These are the total resource requirements for this upgrade. Materials are delivered over %s monthly %s through construction trade contracts.",
                    5f,Color.ORANGE, String.valueOf(months),
                    months == 1 ? "installment" : "installments");
        }

        if (!items.isEmpty()) {
            tooltip.addSectionHeading("Special Items", Alignment.MID, 10f);
            tooltip.addCustom(AoTDToolboxMisc.createCostSection(
                    width, 0f, items, 0f, 30f, 9f, 5f, 10f, false, true), 5f);
            tooltip.addPara("Special items are drawn from storage at player-owned markets in this star system.",
                    Misc.getGrayColor(), 3f);
        }

        if (materials.isEmpty() && items.isEmpty()) {
            tooltip.addPara("No additional construction materials are required.", Misc.getGrayColor(), 5f);
        }

        float credits = Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        if (credits < upgrade.getBuildCostCredits()) {
            tooltip.addPara("You have %s credits. The upgrade cannot begin until you can pay its full cost.",
                    10f, Misc.getNegativeHighlightColor(), Misc.getDGSCredits(credits));
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (confirmButton != null) {
            boolean enabled = canUpgrade();
            if (confirmButton.isEnabled() != enabled) confirmButton.setEnabled(enabled);
            if (!enabled) confirmButton.setChecked(false);
        }
    }

    @Override
    public void applyConfirmScript() {
        if (!canUpgrade()) return;
        AoTDStableStructureImpl current = widget.getStructure();
        // Use ToT's existing staged upgrade implementation; it handles payments,
        // construction contracts, scaffolds, completion, and UI refresh events.
        if (current.startUpgrading(upgrade.id)) {
            widget.requestSystemStructuresRefresh();
        }
    }
}
