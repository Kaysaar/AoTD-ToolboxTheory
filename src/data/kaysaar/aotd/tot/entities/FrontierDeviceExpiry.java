package data.kaysaar.aotd.tot.entities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin;
import data.kaysaar.aotd.tot.plugins.ReflectionUtilis;
import com.fs.starfarer.api.util.DelayedActionScript;

public class FrontierDeviceExpiry extends DelayedActionScript {
    private final SectorEntityToken device;

    public FrontierDeviceExpiry(SectorEntityToken device) {
        super(210f);
        this.device = device;
    }


    public static int getDaysRemaining(SectorEntityToken device) {
        for (EveryFrameScript script : Global.getSector().getScripts()) {
            if (script instanceof FrontierDeviceExpiry expiry && expiry.device == device) {
                return (int) Math.ceil(expiry.daysLeft);
            }
        }
        return 0;
    }

    @Override
    public boolean isDone() {
        return super.isDone() || !device.isAlive();
    }

    @Override
    public void doAction() {
        if (device.getContainingLocation() != null) device.getContainingLocation().removeEntity(device);
        device.setExpired(true);
    }
}
