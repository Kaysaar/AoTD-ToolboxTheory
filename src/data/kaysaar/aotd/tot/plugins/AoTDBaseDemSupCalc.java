package data.kaysaar.aotd.tot.plugins;

import data.kaysaar.aotd.tot.stablestructures.impl.AoTDAPCImpl;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import data.kaysaar.aotd.tot.scripts.economy.AoTDIndustryData;


public class AoTDBaseDemSupCalc implements AoTDDemSupCalc{
        public static float econMult = 0.3f;
        public static final String RAW_PRODUCTION_MULT = "aotd_raw_production_mult";
        private static final float LN2_INV = (float)(1.0 / Math.log(2.0));

        @Override
        public int getRawUnitsFromSupply(MutableStat supply, MarketAPI market, String commodityID,Industry ind) {
            MutableStat statSupply = supply.createCopy();
            MutableStat.StatMod rawMultiplier = statSupply.getMultStatMod(RAW_PRODUCTION_MULT);
            if (rawMultiplier != null) statSupply.unmodifyMult(RAW_PRODUCTION_MULT);
            if (statSupply.getModifiedInt() <= 0 || statSupply.getMult() == 0f) return 0;
            for (AoTDSupDemListener listener : AoTDCommodityEconSpecManager.getListeners()) {
                listener.applyEffectsOnMutableStatCopySupply(statSupply,ind,commodityID);
            }
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityID);;
            float units = statSupply.getModifiedInt();
            if (units > 0f) units += AoTDAPCImpl.outputSizeBonus(ind) * statSupply.getMult() * (1f + statSupply.getPercentMod() / 100f);
            int amount = (int) Math.floor(AoTDCommodityEconSpecManager.getEconSpec(commodityID).getSupplyMult()
                    * units * spec.getEconUnit() * econMult);
            return rawMultiplier == null ? amount : (int) Math.floor(amount * (double) rawMultiplier.value);

        }

        @Override
        public int getRawUnitsFromDemand(MutableStat dem, MarketAPI market, String commodityID,Industry ind) {
            MutableStat statDem = dem.createCopy();
            for (AoTDSupDemListener listener : AoTDCommodityEconSpecManager.getListeners()) {
                listener.applyEffectsOnMutableStatCopyDemand(statDem,ind,commodityID);
            }
            MutableStat.StatMod applied = statDem.getFlatStatMod("ind_dr");
            if (applied != null && ind != null) {
                MutableStat reduction = ind.getDemandReduction();
                if (reduction.getFlatStatMod(AoTDIndustryData.source) != null && applied.value == -reduction.getModifiedInt()) {
                    reduction = reduction.createCopy();
                    reduction.unmodifyFlat(AoTDIndustryData.source);
                    statDem.modifyFlat("ind_dr", -reduction.getModifiedInt());
                }
            }
            int effective = statDem.getModifiedInt();
            if(effective<=0)return 0;
            float supplyMultComd =  AoTDCommodityEconSpecManager.getEconSpec(commodityID).getDemandMult();
            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityID);;
            return (int) Math.floor(supplyMultComd*effective*spec.getEconUnit()*econMult*AoTDAPCImpl.demandMultiplier(ind, commodityID));
        }

        @Override
        public int convertRawUnitsToSupply(float units, MarketAPI market, String commodityID) {

            if (units <= 0) return 0;

            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityID);
            float supplyMult = AoTDCommodityEconSpecManager
                    .getEconSpec(commodityID)
                    .getSupplyMult();

            float denom = supplyMult * spec.getEconUnit() * econMult;

            if (denom <= 0f) return 0;

            float base = units / denom;

            if (base <= 0f) return 0;



            // ---------- LINEAR MODE ----------
            return (int) Math.floor(base);
        }


        @Override
        public int convertRawUnitsToDemand(float units, MarketAPI market, String commodityID) {

            if (units <= 0) return 0;

            CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(commodityID);
            float demandMult = AoTDCommodityEconSpecManager
                    .getEconSpec(commodityID)
                    .getDemandMult();

            float denom = demandMult * spec.getEconUnit() * econMult;

            if (denom <= 0f) return 0;

            float base = units / denom;

            if (base <= 0f) return 0;


            // ---------- LINEAR MODE ----------
            return (int) Math.floor(base);
        }

    }

