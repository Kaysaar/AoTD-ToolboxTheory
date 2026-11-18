package data.kaysaar.aotd.tot.ui.production;

import ashlib.data.plugins.ui.models.BasePopUpDialog;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipeManager;
import data.kaysaar.aotd.tot.recipes.AoTDIndustryRecipePlugin;
import data.kaysaar.aotd.tot.recipes.AoTDProductionLineManager;

public class ProductionPlanChangeDialog extends BasePopUpDialog {
    public static final float WIDTH = 1150f;
    private final IndustrySpecAPI spec;
    private final Industry target;
    private final Runnable onChanged;
    private ProductionPlanChangePanel content;
    private float availabilityTimer;

    public ProductionPlanChangeDialog(IndustrySpecAPI spec, Runnable onChanged) {
        super("Change Production Plan");
        this.target = null;
        this.spec = spec;
        this.onChanged = onChanged;
    }

    public ProductionPlanChangeDialog(Industry target) {
        super("Change Production Plan - " + target.getMarket().getName());
        this.target = target;
        this.spec = target.getSpec();
        this.onChanged = null;
    }

    public ProductionPlanChangePanel getContent() {
        return content;
    }

    @Override
    public void createUI(CustomPanelAPI panel) {
        createHeaader(panel);
        float width = panel.getPosition().getWidth() - 30f;
        String currentId;
        if (target == null) {
            currentId = AoTDProductionLineManager.getRecipeId(spec.getId());
        } else {
            AoTDIndustryRecipePlugin active = AoTDIndustryRecipeManager.getInstance().getActiveRecipe(target);
            currentId = active == null ? null : active.getId();
        }
        content = new ProductionPlanChangePanel(width, panel.getPosition().getHeight() - y - 50f,
                spec, currentId, target);
        panel.addComponent(content.getMainPanel()).inTL(x, y);
        addPanel(content.getMainPanel());
        createConfirmAndCancelSection(panel);
        confirmButton.setEnabled(content.canConfirm());
    }

    @Override
    public void advance(float amount) {
        if (content != null && confirmButton != null) {
            availabilityTimer += amount;
            if (availabilityTimer >= 0.5f || confirmButton.isChecked()) {
                availabilityTimer = 0f;
                content.refreshAvailability();
            }
            boolean allowed = content.canConfirm();
            confirmButton.setEnabled(allowed);
            // PopUpUI closes after a checked Confirm, so clear stale/invalid clicks before it handles them.
            if (!allowed && confirmButton.isChecked()) {
                confirmButton.setChecked(false);
                content.setStatus(content.getUnavailableReason());
            }
        }
        super.advance(amount);
    }

    @Override
    public void applyConfirmScript() {
        if (content == null || !content.canConfirm()) return;
        if (target != null) {
            AoTDProductionLineManager.queueLocalPlan(target, content.getSelectedRecipeId());
            return;
        }
        AoTDProductionLineManager.setRecipeId(spec.getId(), content.getSelectedRecipeId());
        if (onChanged != null) onChanged.run();
    }
}
