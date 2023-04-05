package com.idevhub.coino.tradingengine.service.settings;

import com.idevhub.coino.settings.entity.SettingItem;
import com.idevhub.coino.settings.entity.SettingItemInfo;
import com.idevhub.coino.settings.service.SettingsInfoProvider;
import com.idevhub.coino.settings.service.helper.SettingHelper;
import com.idevhub.coino.tradingmodel.entity.enumeration.MarketStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;

@Service
public class TradingEngineSettingInfoProvider extends SettingsInfoProvider {
    public TradingEngineSettingInfoProvider(SettingHelper helper) {
        super(helper);
    }

    @Override
    public List<SettingItemInfo<? extends SettingItem>> getAllSettingInfo() {
        return asList(new SettingItemInfo<>(TradingEngineSettings.class,
            new HashMap<TradingEngineSettings, Class<?>>() {{
                put(TradingEngineSettings.MARKET_STATUS, MarketStatus.class);
                put(TradingEngineSettings.MIN_AMOUNT, BigDecimal.class);
            }}));
    }
}
